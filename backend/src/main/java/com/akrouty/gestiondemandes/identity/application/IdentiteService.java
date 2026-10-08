package com.akrouty.gestiondemandes.identity.application;

import com.akrouty.gestiondemandes.identity.domain.EmailNormalizer;
import com.akrouty.gestiondemandes.identity.domain.Role;
import com.akrouty.gestiondemandes.identity.domain.RoleAdministrateurNonAttribuableException;
import com.akrouty.gestiondemandes.identity.domain.Utilisateur;
import com.akrouty.gestiondemandes.identity.persistence.UtilisateurRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsabilité publique applicative du module {@code identity} :
 * orchestration des cas d'utilisation d'administration et d'identification,
 * port des transactions (ADR-002).
 *
 * <p>C'est cette responsabilité — et jamais {@link UtilisateurRepository} —
 * que le module {@code security} consomme pour : rechercher l'identité au
 * login, et relire l'utilisateur (existence, {@code actif}, rôles actuels)
 * à chaque requête protégée.</p>
 *
 * <p>La normalisation d'email est toujours {@link EmailNormalizer} (trim +
 * lowercase {@code Locale.ROOT}), y compris pour la recherche
 * d'authentification : une seule stratégie, appliquée ici.</p>
 */
@Service
public class IdentiteService {

	private final UtilisateurRepository repository;
	private final PasswordEncoder passwordEncoder;
	private final MotDePasseProperties motDePasseProperties;

	public IdentiteService(
			UtilisateurRepository repository,
			PasswordEncoder passwordEncoder,
			MotDePasseProperties motDePasseProperties) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
		this.motDePasseProperties = motDePasseProperties;
	}

	// ---------------------------------------------------------------- écritures

	/**
	 * Crée un utilisateur : politique de mot de passe puis hachage immédiat
	 * (le mot de passe en clair n'est jamais persisté, ni journalisé), email
	 * normalisé, unicité contrôlée, rôles limités aux rôles métier.
	 */
	@Transactional
	public UtilisateurConsultation creer(CreationUtilisateurCommand commande) {
		validerRolesMetier(commande.rolesMetier());
		validerMotDePasse(commande.password());

		String emailNormalise = EmailNormalizer.normalize(commande.email());
		if (repository.existsByEmail(emailNormalise)) {
			throw new EmailDejaUtiliseException();
		}
		String passwordHash = passwordEncoder.encode(commande.password());
		Utilisateur utilisateur = repository.save(new Utilisateur(
				commande.nom(),
				emailNormalise,
				commande.actif(),
				passwordHash,
				commande.rolesMetier()));
		return consulter(utilisateur);
	}

	/**
	 * Modifie les informations générales uniquement : ni {@code actif}, ni
	 * rôles, ni {@code passwordHash} ne sont affectés (opérations dédiées).
	 */
	@Transactional
	public UtilisateurConsultation modifier(Long id, String nom, String email) {
		Utilisateur utilisateur = charger(id);
		String emailNormalise = EmailNormalizer.normalize(email);
		if (repository.existsByEmailAndIdNot(emailNormalise, id)) {
			throw new EmailDejaUtiliseException();
		}
		utilisateur.modifierInformations(nom, emailNormalise);
		return consulter(utilisateur);
	}

	@Transactional
	public UtilisateurConsultation activer(Long id) {
		Utilisateur utilisateur = charger(id);
		utilisateur.activer();
		return consulter(utilisateur);
	}

	@Transactional
	public UtilisateurConsultation desactiver(Long id, Long acteurId) {
		if (id.equals(acteurId)) {
			throw new AutoDesactivationInterditeException();
		}
		Utilisateur utilisateur = charger(id);
		utilisateur.desactiver();
		return consulter(utilisateur);
	}

	/**
	 * Remplace les rôles métier RT/AT ; un rôle ADMINISTRATEUR existant est
	 * conservé et ne peut être ni attribué ni retiré par cette opération.
	 */
	@Transactional
	public UtilisateurConsultation remplacerRolesMetier(Long id, Set<Role> rolesMetier) {
		Utilisateur utilisateur = charger(id);
		utilisateur.remplacerRolesMetier(rolesMetier == null ? Set.of() : rolesMetier);
		return consulter(utilisateur);
	}

	// ---------------------------------------------------------------- lectures

	@Transactional(readOnly = true)
	public UtilisateurConsultation obtenir(Long id) {
		return consulter(charger(id));
	}

	/**
	 * Utilisateur interne EXISTANT chargé en entité pour les relations JPA des
	 * demandes (créateur, Agent affecté, auteur d'historique) — responsabilité
	 * publique étendue pour le module {@code request} (Bloc 3), sans exposer
	 * {@link UtilisateurRepository}.
	 *
	 * @throws UtilisateurIntrouvableException si l'id n'existe pas
	 */
	@Transactional(readOnly = true)
	public Utilisateur obtenirUtilisateur(Long id) {
		return charger(id);
	}

	/**
	 * Liste paginée ; les rôles sont chargés par une requête ciblée
	 * (pas de N+1, pas d'EAGER global).
	 */
	@Transactional(readOnly = true)
	public Page<UtilisateurConsultation> lister(Pageable pageable) {
		Page<Utilisateur> page = repository.findAll(pageable);
		List<Long> ids = page.getContent().stream().map(Utilisateur::getId).toList();
		if (!ids.isEmpty()) {
			// Même transaction : le join fetch réutilise les instances de la page
			// et initialise leurs rôles sans lecture par ligne.
			repository.findAllByIdAvecRoles(ids);
		}
		List<UtilisateurConsultation> items = page.getContent().stream()
				.map(this::consulter)
				.toList();
		return new PageImpl<>(items, pageable, page.getTotalElements());
	}

	/**
	 * Recherche d'identité pour l'authentification : email normalisé à
	 * l'entrée ; {@code passwordHash} retourne uniquement pour la vérification
	 * {@code PasswordEncoder.matches} du login.
	 */
	@Transactional(readOnly = true)
	public Optional<IdentiteConnexion> rechercherPourConnexion(String email) {
		String emailNormalise = EmailNormalizer.normalize(email);
		return repository.findByEmail(emailNormalise)
				.map(u -> new IdentiteConnexion(
						u.getId(), u.getNom(), u.getEmail(), u.isActif(), u.getRoles(), u.getPasswordHash()));
	}

	/**
	 * Relecture d'identité pour une requête JWT : existence, état {@code actif}
	 * et rôles ACTUELS depuis la base, à chaque appel. Le JWT n'est jamais une
	 * source d'autorité.
	 */
	@Transactional(readOnly = true)
	public Optional<UtilisateurConsultation> chargerIdentite(Long id) {
		return repository.findById(id).map(this::consulter);
	}

	/**
	 * Snapshots non modifiables d'un ensemble restreint d'utilisateurs, avec
	 * rôles chargés par requête ciblée (pas de N+1). Permet au module
	 * {@code request} de construire ses projections de consultation sans
	 * dépendance à {@link UtilisateurRepository}.
	 */
	@Transactional(readOnly = true)
	public List<UtilisateurConsultation> consulterUtilisateurs(Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}
		return repository.findAllByIdAvecRoles(ids).stream()
				.map(this::consulter)
				.toList();
	}

	/**
	 * Agents affectables (Bloc 3) : uniquement les utilisateurs ACTIFS
	 * possédant le rôle {@code AGENT_TECHNIQUE}. Les rôles sont chargés par une
	 * requête ciblée puis filtrés ; aucun credential n'est exposé.
	 */
	@Transactional(readOnly = true)
	public List<UtilisateurConsultation> listerAgentsActifs() {
		return repository.findAllActifsAvecRoles().stream()
				.filter(utilisateur -> utilisateur.getRoles().contains(Role.AGENT_TECHNIQUE))
				.map(this::consulter)
				.toList();
	}

	// ---------------------------------------------------------------- privé

	private Utilisateur charger(Long id) {
		return repository.findById(id).orElseThrow(() -> new UtilisateurIntrouvableException(id));
	}

	/** Snapshot non modifiable, matérialisé dans la transaction en cours. */
	private UtilisateurConsultation consulter(Utilisateur utilisateur) {
		return new UtilisateurConsultation(
				utilisateur.getId(),
				utilisateur.getNom(),
				utilisateur.getEmail(),
				utilisateur.isActif(),
				utilisateur.getRoles());
	}

	/** Le mot de passe est présent, non blanc et respecte la longueur minimale configurée. */
	private void validerMotDePasse(String password) {
		if (password == null || password.isBlank()) {
			throw new MotDePasseNonConformeException();
		}
		if (password.length() < motDePasseProperties.minLength()) {
			throw new MotDePasseNonConformeException();
		}
	}

	/** À la création, seuls RT et AT sont acceptés : jamais ADMINISTRATEUR (RM47, RM48). */
	private void validerRolesMetier(Set<Role> roles) {
		if (roles == null) {
			return;
		}
		if (roles.contains(Role.ADMINISTRATEUR)) {
			throw new RoleAdministrateurNonAttribuableException();
		}
	}
}
