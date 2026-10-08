package com.akrouty.gestiondemandes.request.application;

import com.akrouty.gestiondemandes.identity.application.IdentiteService;
import com.akrouty.gestiondemandes.identity.application.UtilisateurConsultation;
import com.akrouty.gestiondemandes.identity.domain.Role;
import com.akrouty.gestiondemandes.identity.domain.Utilisateur;
import com.akrouty.gestiondemandes.request.domain.Categorie;
import com.akrouty.gestiondemandes.request.domain.Client;
import com.akrouty.gestiondemandes.request.domain.DemandeTechnique;
import com.akrouty.gestiondemandes.request.domain.EvenementsDemande;
import com.akrouty.gestiondemandes.request.domain.Priorite;
import com.akrouty.gestiondemandes.request.domain.StatutDemande;
import com.akrouty.gestiondemandes.request.persistence.ClientRepository;
import com.akrouty.gestiondemandes.request.persistence.DemandeSpecifications;
import com.akrouty.gestiondemandes.request.persistence.DemandeTechniqueRepository;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'utilisation du module {@code request} (ADR-002) :
 * orchestration, transactions, contrôles métier contextuels et collaboration
 * avec {@code identity} via {@link IdentiteService} — jamais via
 * {@code UtilisateurRepository}.
 *
 * <p>Toute opération de modification est {@code @Transactional} : la
 * modification métier, les dates, l'historisation et la persistance forment
 * une seule unité atomique. Si l'opération échoue, aucun événement parasite
 * n'est persisté.</p>
 *
 * <p>Le périmètre de lecture de l'AT est appliqué comme contrainte
 * SUPPLÉMENTAIRE aux filtres : un filtre ne l'élargit jamais.</p>
 */
@Service
public class DemandeService {

	private final DemandeTechniqueRepository demandes;
	private final ClientRepository clients;
	private final IdentiteService identite;
	private final GenerateurReferenceDemande generateurReference;

	public DemandeService(
			DemandeTechniqueRepository demandes,
			ClientRepository clients,
			IdentiteService identite,
			GenerateurReferenceDemande generateurReference) {
		this.demandes = demandes;
		this.clients = clients;
		this.identite = identite;
		this.generateurReference = generateurReference;
	}

	// ---------------------------------------------------------------- création

	/**
	 * Enregistrement d'une demande (RM03–RM08) : client existant OU nouveau
	 * client (jamais les deux), référence générée serveur (UUID v4), statut
	 * imposé {@code NOUVELLE}, créateur = utilisateur authentifié, dates
	 * serveur, création historisée — le tout dans une seule transaction.
	 */
	@Transactional
	public DemandeConsultation creer(Long createurId, CreationDemandeCommand commande) {
		if ((commande.clientId() == null) == (commande.nouveauClient() == null)) {
			throw new RequeteInvalidException("Exactement un de clientId ou nouveauClient doit être fourni.");
		}
		Utilisateur createur = identite.obtenirUtilisateur(createurId);
		Client client = resoudreClient(commande);
		String reference = generateurReference.generer(demandes::existsByReference);
		Instant maintenant = Instant.now();

		DemandeTechnique demande = DemandeTechnique.creer(
				reference,
				commande.titre(),
				commande.description(),
				commande.categorie(),
				commande.priorite(),
				client,
				createur,
				maintenant);
		demande.ajouterEvenement(maintenant, EvenementsDemande.CREATION, null,
				StatutDemande.NOUVELLE.name(), createur);
		demandes.save(demande);
		return verserDetail(demande);
	}

	private Client resoudreClient(CreationDemandeCommand commande) {
		if (commande.clientId() != null) {
			return clients.findById(commande.clientId())
					.orElseThrow(() -> new ClientIntrouvableException(commande.clientId()));
		}
		// RM07/RM08 : persistance du nouveau client, AUCUNE déduplication
		// (l'email d'un Client n'est pas unique — ADR-003).
		CreationDemandeCommand.NouveauClientCommand nouveau = commande.nouveauClient();
		return clients.save(new Client(nouveau.nom(), nouveau.email(), nouveau.telephone()));
	}

	// ---------------------------------------------------------------- qualification

	/** Qualification (RM13, RM21) : réservée à une demande fonctionnellement modifiable. */
	@Transactional
	public DemandeConsultation qualifier(Long acteurId, String reference, Categorie categorie, Priorite priorite) {
		DemandeTechnique demande = charger(reference);
		Utilisateur auteur = identite.obtenirUtilisateur(acteurId);
		demande.qualifier(categorie, priorite, auteur, Instant.now());
		demandes.save(demande);
		return verserDetail(demande);
	}

	// ---------------------------------------------------------------- affectation

	/**
	 * Affectation / réaffectation (RM29, RM34, RM35) : la cible doit exister
	 * ({@code 404}), être active et posséder {@code AGENT_TECHNIQUE}
	 * ({@code 409} sinon).
	 */
	@Transactional
	public DemandeConsultation affecter(Long acteurId, String reference, Long agentId) {
		DemandeTechnique demande = charger(reference);
		Utilisateur agent = identite.obtenirUtilisateur(agentId);
		if (!agent.isActif() || !agent.getRoles().contains(Role.AGENT_TECHNIQUE)) {
			throw new AgentNonAffectableException("L'Agent cible doit être actif et posséder le rôle AGENT_TECHNIQUE.");
		}
		Utilisateur auteur = identite.obtenirUtilisateur(acteurId);
		demande.affecter(agent, auteur, Instant.now());
		demandes.save(demande);
		return verserDetail(demande);
	}
	// ---------------------------------------------------------------- traitements & fin de cycle

	/**
	 * Démarrage du traitement (RM30) : contrôle contextuel OBLIGATOIRE —
	 * l'utilisateur authentifié doit être l'Agent affecté, sinon {@code 403}.
	 */
	@Transactional
	public DemandeConsultation demarrerTraitement(Long acteurId, String reference) {
		DemandeTechnique demande = charger(reference);
		verifierAgentAffecte(demande, acteurId);
		Utilisateur auteur = identite.obtenirUtilisateur(acteurId);
		demande.demarrerTraitement(auteur, Instant.now());
		demandes.save(demande);
		return verserDetail(demande);
	}

	/**
	 * Mise à jour traitement / solution (RM32) : contrôle contextuel — seul
	 * l'Agent affecté, sur une demande {@code EN_COURS}.
	 */
	@Transactional
	public DemandeConsultation majTraitement(Long acteurId, String reference, TraitementCommand commande) {
		DemandeTechnique demande = charger(reference);
		verifierAgentAffecte(demande, acteurId);
		Utilisateur auteur = identite.obtenirUtilisateur(acteurId);
		demande.majTraitement(
				commande.descriptionTraitement(), commande.descriptionFournie(),
				commande.solution(), commande.solutionFournie(),
				auteur, Instant.now());
		demandes.save(demande);
		return verserDetail(demande);
	}

	/** Résolution (RM33, RM36) : Agent affecté, {@code EN_COURS}, solution présente. */
	@Transactional
	public DemandeConsultation resoudre(Long acteurId, String reference) {
		DemandeTechnique demande = charger(reference);
		verifierAgentAffecte(demande, acteurId);
		Utilisateur auteur = identite.obtenirUtilisateur(acteurId);
		demande.resoudre(auteur, Instant.now());
		demandes.save(demande);
		return verserDetail(demande);
	}

	/** Refus de résolution (RM38) : {@code RESOLUE → EN_COURS}, traitement et solution conservés. */
	@Transactional
	public DemandeConsultation refuserResolution(Long acteurId, String reference) {
		DemandeTechnique demande = charger(reference);
		Utilisateur auteur = identite.obtenirUtilisateur(acteurId);
		demande.refuserResolution(auteur, Instant.now());
		demandes.save(demande);
		return verserDetail(demande);
	}

	/** Clôture (RM37) : {@code RESOLUE → CLOTUREE}, état terminal. */
	@Transactional
	public DemandeConsultation cloturer(Long acteurId, String reference) {
		DemandeTechnique demande = charger(reference);
		Utilisateur auteur = identite.obtenirUtilisateur(acteurId);
		demande.cloturer(auteur, Instant.now());
		demandes.save(demande);
		return verserDetail(demande);
	}

	/** Annulation (RM39–RM42) : motif obligatoire, états autorisés uniquement. */
	@Transactional
	public DemandeConsultation annuler(Long acteurId, String reference, String motif) {
		if (motif == null || motif.isBlank()) {
			throw new RequeteInvalidException("Le motif d'annulation est obligatoire.");
		}
		DemandeTechnique demande = charger(reference);
		Utilisateur auteur = identite.obtenirUtilisateur(acteurId);
		demande.annuler(motif, auteur, Instant.now());
		demandes.save(demande);
		return verserDetail(demande);
	}
	// ---------------------------------------------------------------- consultations

	/**
	 * Détail : RT consulte toute demande ; AT uniquement celle dont il est
	 * l'Agent affecté ({@code 403} sinon) ; demande inexistante → {@code 404}.
	 */
	@Transactional(readOnly = true)
	public DemandeConsultation consulter(Long acteurId, Set<Role> roles, String reference) {
		DemandeTechnique demande = charger(reference);
		if (!roles.contains(Role.RESPONSABLE_TECHNIQUE)) {
			verifierAgentAffecte(demande, acteurId);
		}
		return verserDetail(demande);
	}

	/**
	 * Liste paginée : RT voit tout ; AT ne voit que ses demandes. Les filtres
	 * sont combinés par ET avec le périmètre — jamais en remplacement.
	 */
	@Transactional(readOnly = true)
	public Page<DemandeResumeConsultation> lister(
			Long acteurId, Set<Role> roles, FiltresDemande filtres, Pageable pageable) {
		Specification<DemandeTechnique> specification = DemandeSpecifications.avecFiltres(
				filtres.statut(), filtres.priorite(), filtres.categorie(),
				filtres.clientId(), filtres.agentId(), filtres.recherche());
		if (!roles.contains(Role.RESPONSABLE_TECHNIQUE)) {
			specification = specification.and(DemandeSpecifications.perimetreAgent(acteurId));
		}
		Page<DemandeTechnique> page = demandes.findAll(specification, pageable);
		List<DemandeTechnique> contenu = page.getContent();
		if (contenu.isEmpty()) {
			return page.map(d -> verserResume(d, Map.of()));
		}
		demandes.chargerConsultation(contenu.stream().map(DemandeTechnique::getId).toList());
		Map<Long, UtilisateurConsultation> agents = indexerUtilisateurs(
				contenu.stream()
						.map(DemandeTechnique::getAgentAffecte)
						.filter(Objects::nonNull)
						.map(Utilisateur::getId)
						.toList());
		return page.map(demande -> verserResume(demande, agents));
	}

	/** Recherche simple de clients pour la sélection à la création (aucun CRUD autonome). */
	@Transactional(readOnly = true)
	public Page<ClientConsultation> listerClients(String recherche, Pageable pageable) {
		Page<Client> page = (recherche == null || recherche.isBlank())
				? clients.findAll(pageable)
				: clients.rechercher(recherche.trim(), pageable);
		return page.map(c -> new ClientConsultation(c.getId(), c.getNom(), c.getEmail(), c.getTelephone()));
	}

	/** Liste des Agents affectables : actifs ET possédant {@code AGENT_TECHNIQUE} (via Identity). */
	@Transactional(readOnly = true)
	public List<UtilisateurConsultation> listerAgents() {
		return identite.listerAgentsActifs();
	}
	// ---------------------------------------------------------------- privé

	private DemandeTechnique charger(String reference) {
		return demandes.findByReference(reference)
				.orElseThrow(() -> new DemandeIntrouvableException(reference));
	}

	/** Contrôle contextuel : seul l'Agent affecté à la demande ciblé peut agir (RM31). */
	private void verifierAgentAffecte(DemandeTechnique demande, Long acteurId) {
		Utilisateur agent = demande.getAgentAffecte();
		if (agent == null || !agent.getId().equals(acteurId)) {
			throw new AccesDemandeInterditException(
					"Seul l'Agent technique affecté à la demande peut réaliser cette action.");
		}
	}

	private Map<Long, UtilisateurConsultation> indexerUtilisateurs(List<Long> ids) {
		Set<Long> distincts = new LinkedHashSet<>(ids);
		return identite.consulterUtilisateurs(distincts).stream()
				.collect(Collectors.toMap(UtilisateurConsultation::id, u -> u, (a, b) -> a));
	}

	/** Détail : client + createur + agent affecté, rôles chargés par requête ciblée. */
	private DemandeConsultation verserDetail(DemandeTechnique demande) {
		Set<Long> ids = new LinkedHashSet<>();
		ids.add(demande.getCreateur().getId());
		if (demande.getAgentAffecte() != null) {
			ids.add(demande.getAgentAffecte().getId());
		}
		Map<Long, UtilisateurConsultation> utilisateurs = indexerUtilisateurs(List.copyOf(ids));
		Client c = demande.getClient();
		return new DemandeConsultation(
				demande.getReference(),
				demande.getTitre(),
				demande.getDescription(),
				demande.getCategorie(),
				demande.getPriorite(),
				demande.getStatut(),
				new ClientConsultation(c.getId(), c.getNom(), c.getEmail(), c.getTelephone()),
				utilisateurs.get(demande.getCreateur().getId()),
				demande.getAgentAffecte() == null
						? null
						: utilisateurs.get(demande.getAgentAffecte().getId()),
				demande.getDescriptionTraitement(),
				demande.getSolution(),
				demande.getMotifAnnulation(),
				demande.getDateCreation(),
				demande.getDateModification(),
				demande.getDateResolution(),
				demande.getDateCloture(),
				demande.getDateAnnulation());
	}

	private DemandeResumeConsultation verserResume(
			DemandeTechnique demande, Map<Long, UtilisateurConsultation> agents) {
		Client c = demande.getClient();
		Utilisateur agent = demande.getAgentAffecte();
		return new DemandeResumeConsultation(
				demande.getReference(),
				demande.getTitre(),
				demande.getCategorie(),
				demande.getPriorite(),
				demande.getStatut(),
				new ClientConsultation(c.getId(), c.getNom(), c.getEmail(), c.getTelephone()),
				agent == null ? null : agents.get(agent.getId()),
				demande.getDateCreation(),
				demande.getDateModification());
	}
}
