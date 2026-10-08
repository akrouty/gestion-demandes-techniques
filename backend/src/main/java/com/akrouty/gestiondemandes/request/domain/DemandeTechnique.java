package com.akrouty.gestiondemandes.request.domain;

import com.akrouty.gestiondemandes.identity.domain.Utilisateur;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Demande technique persistée (module {@code request}).
 *
 * <p>{@code reference} est la référence métier : obligatoire, unique, distincte
 * de l'id technique et immuable après création (aucun setter public,
 * colonne non updatable). Son format exact n'est pas validé dans ce bloc ; la
 * génération réelle sera implémentée avec le cas d'utilisation de création.</p>
 */
@Entity
@Table(
		name = "demande_technique",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_demande_technique_reference",
				columnNames = "reference"
		)
)
@SequenceGenerator(name = "demande_technique_seq", sequenceName = "demande_technique_seq", allocationSize = 1)
public class DemandeTechnique {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "demande_technique_seq")
	private Long id;

	@Column(nullable = false, updatable = false)
	private String reference;

	@Column(nullable = false)
	private String titre;

	@Column(nullable = false, columnDefinition = "text")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Categorie categorie;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Priorite priorite;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private StatutDemande statut;

	@Column(columnDefinition = "text")
	private String descriptionTraitement;

	@Column(columnDefinition = "text")
	private String solution;

	@Column(columnDefinition = "text")
	private String motifAnnulation;

	@Column(nullable = false)
	private Instant dateCreation;

	@Column(nullable = false)
	private Instant dateModification;

	private Instant dateResolution;

	private Instant dateCloture;

	private Instant dateAnnulation;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "client_id", nullable = false)
	private Client client;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "createur_id", nullable = false)
	private Utilisateur createur;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "agent_affecte_id")
	private Utilisateur agentAffecte;

	@OneToMany(mappedBy = "demande", cascade = CascadeType.PERSIST, orphanRemoval = false, fetch = FetchType.LAZY)
	private List<HistoriqueDemande> historique = new ArrayList<>();

	/**
	 * Aucun constructeur public fonctionnel : la création métier passe uniquement
	 * par {@link #creer} qui impose {@code NOUVELLE}. Seul le constructeur JPA
	 * protégé sans argument subsiste.
	 */
	protected DemandeTechnique() {
	}

	/**
	 * Opération de création d'une nouvelle demande (RM05).
	 *
	 * <p>La création impose {@code statut = NOUVELLE}, positionne
	 * {@code dateCreation} et {@code dateModification} à l'instant serveur et
	 * laisse les autres dates métier à {@code null}. Le client REST ne choisit
	 * jamais le statut, la référence, le créateur ni les dates.</p>
	 *
	 * @param reference référence générée côté serveur (UUID v4), obligatoire et immuable
	 * @param maintenant instant serveur de création
	 */
	public static DemandeTechnique creer(
			String reference,
			String titre,
			String description,
			Categorie categorie,
			Priorite priorite,
			Client client,
			Utilisateur createur,
			Instant maintenant) {
		Objects.requireNonNull(reference, "reference obligatoire");
		Objects.requireNonNull(titre, "titre obligatoire");
		Objects.requireNonNull(description, "description obligatoire");
		Objects.requireNonNull(categorie, "categorie obligatoire");
		Objects.requireNonNull(priorite, "priorite obligatoire");
		Objects.requireNonNull(client, "client obligatoire");
		Objects.requireNonNull(createur, "createur obligatoire");
		Objects.requireNonNull(maintenant, "dateCreation obligatoire");

		DemandeTechnique demande = new DemandeTechnique();
		demande.reference = reference;
		demande.titre = titre;
		demande.description = description;
		demande.categorie = categorie;
		demande.priorite = priorite;
		demande.statut = StatutDemande.NOUVELLE;
		demande.client = client;
		demande.createur = createur;
		demande.dateCreation = maintenant;
		demande.dateModification = maintenant;
		return demande;
	}

	// ------------------------------------------------- invariants de cycle de vie

	/** RM43 : {@code CLOTUREE} et {@code ANNULEE} sont terminales et non modifiables. */
	private void verifierFonctionnellementModifiable() {
		if (statut == StatutDemande.CLOTUREE || statut == StatutDemande.ANNULEE) {
			throw new DemandeTermineeException(reference);
		}
	}

	/**
	 * Qualification (RM13, RM21) : réservée à une demande fonctionnellement
	 * modifiable. Seuls les changements réels sont appliqués et historisés :
	 * une valeur identique ne produit aucun événement parasite.
	 */
	public void qualifier(Categorie nouvelleCategorie, Priorite nouvellePriorite, Utilisateur auteur, Instant maintenant) {
		verifierFonctionnellementModifiable();
		Objects.requireNonNull(nouvelleCategorie, "categorie obligatoire");
		Objects.requireNonNull(nouvellePriorite, "priorite obligatoire");
		Objects.requireNonNull(auteur, "auteur obligatoire");
		Objects.requireNonNull(maintenant, "instant obligatoire");

		boolean categorieChangee = categorie != nouvelleCategorie;
		boolean prioriteChangee = priorite != nouvellePriorite;
		if (!categorieChangee && !prioriteChangee) {
			return;
		}
		if (categorieChangee) {
			ajouterEvenement(maintenant, EvenementsDemande.CATEGORIE_MODIFIEE, categorie.name(), nouvelleCategorie.name(), auteur);
			categorie = nouvelleCategorie;
		}
		if (prioriteChangee) {
			ajouterEvenement(maintenant, EvenementsDemande.PRIORITE_MODIFIEE, priorite.name(), nouvellePriorite.name(), auteur);
			priorite = nouvellePriorite;
		}
		dateModification = maintenant;
	}

	/**
	 * Affectation / réaffectation (RM29, RM34, RM35) :
	 *
	 * <ul>
	 *   <li>états autorisés : {@code NOUVELLE}, {@code ASSIGNEE}, {@code EN_COURS} ;</li>
	 *   <li>{@code NOUVELLE → ASSIGNEE} ;</li>
	 *   <li>{@code ASSIGNEE → ASSIGNEE} lors d'une vraie réaffectation ;</li>
	 *   <li>{@code EN_COURS → ASSIGNEE} lors d'une réaffectation vers UN AUTRE
	 *       Agent (RM34) ;</li>
	 *   <li>un même Agent déjà affecté ne déclenche aucune réaffectation ni
	 *       aucun événement ;</li>
	 *   <li>les données de traitement / solution ne sont jamais supprimées.</li>
	 * </ul>
	 */
	public void affecter(Utilisateur agent, Utilisateur auteur, Instant maintenant) {
		verifierFonctionnellementModifiable();
		Objects.requireNonNull(agent, "agent obligatoire");
		Objects.requireNonNull(auteur, "auteur obligatoire");
		Objects.requireNonNull(maintenant, "instant obligatoire");

		if (statut != StatutDemande.NOUVELLE && statut != StatutDemande.ASSIGNEE && statut != StatutDemande.EN_COURS) {
			throw new TransitionInvalidException(
					"Affectation impossible depuis l'état " + statut + ".");
		}
		boolean memeAgent = agentAffecte != null && Objects.equals(agentAffecte.getId(), agent.getId());
		if (memeAgent) {
			// Pas de fausse réaffectation pour le même Agent (RM34) :
			// aucun changement de statut, aucun événement, aucune remise à zéro.
			return;
		}
		StatutDemande avant = statut;
		Utilisateur ancien = agentAffecte;
		agentAffecte = agent;
		if (avant == StatutDemande.NOUVELLE || avant == StatutDemande.EN_COURS) {
			// NOUVELLE → ASSIGNEE ; RM34 : EN_COURS réaffecté à un AUTRE Agent → ASSIGNEE.
			// RM35 : ASSIGNEE → ASSIGNEE est conservé (branche non modifiée).
			statut = StatutDemande.ASSIGNEE;
		}
		dateModification = maintenant;
		ajouterEvenement(
				maintenant,
				avant == StatutDemande.NOUVELLE ? EvenementsDemande.AFFECTATION : EvenementsDemande.REAFFECTATION,
				ancien == null ? null : ancien.getEmail(),
				agent.getEmail(),
				auteur);
	}

	/**
	 * Démarrage du traitement : {@code ASSIGNEE → EN_COURS} (RM30). Un Agent
	 * affecté est obligatoire avant {@code EN_COURS}.
	 */
	public void demarrerTraitement(Utilisateur auteur, Instant maintenant) {
		verifierFonctionnellementModifiable();
		Objects.requireNonNull(auteur, "auteur obligatoire");
		Objects.requireNonNull(maintenant, "instant obligatoire");

		if (statut != StatutDemande.ASSIGNEE) {
			throw new TransitionInvalidException(
					"Le démarrage du traitement exige l'état ASSIGNEE, état actuel : " + statut + ".");
		}
		if (agentAffecte == null) {
			throw new TransitionInvalidException("Aucun Agent affecté.");
		}
		statut = StatutDemande.EN_COURS;
		dateModification = maintenant;
		ajouterEvenement(maintenant, EvenementsDemande.TRAITEMENT_DEMARRE,
				StatutDemande.ASSIGNEE.name(), StatutDemande.EN_COURS.name(), auteur);
	}

	/**
	 * Mise à jour de la description du traitement et/ou de la solution.
	 * Chaque champ fourni remplace la valeur existante ; un champ non fourni
	 * reste inchangé. Seuls les changements réels sont historisés.
	 * État obligatoire : {@code EN_COURS}.
	 */
	public void majTraitement(
			String nouvelleDescriptionTraitement,
			boolean descriptionFournie,
			String nouvelleSolution,
			boolean solutionFournie,
			Utilisateur auteur,
			Instant maintenant) {
		verifierFonctionnellementModifiable();
		Objects.requireNonNull(auteur, "auteur obligatoire");
		Objects.requireNonNull(maintenant, "instant obligatoire");

		if (statut != StatutDemande.EN_COURS) {
			throw new TransitionInvalidException(
					"La mise à jour du traitement exige l'état EN_COURS, état actuel : " + statut + ".");
		}
		boolean change = false;
		if (descriptionFournie && !Objects.equals(descriptionTraitement, nouvelleDescriptionTraitement)) {
			ajouterEvenement(maintenant, EvenementsDemande.DESCRIPTION_TRAITEMENT_MODIFIEE,
					descriptionTraitement, nouvelleDescriptionTraitement, auteur);
			descriptionTraitement = nouvelleDescriptionTraitement;
			change = true;
		}
		if (solutionFournie && !Objects.equals(solution, nouvelleSolution)) {
			ajouterEvenement(maintenant, EvenementsDemande.SOLUTION_MODIFIEE,
					solution, nouvelleSolution, auteur);
			solution = nouvelleSolution;
			change = true;
		}
		if (change) {
			dateModification = maintenant;
		}
	}

	/**
	 * Résolution (RM33) : {@code EN_COURS → RESOLUE}, uniquement si une solution
	 * non blanche existe.
	 */
	public void resoudre(Utilisateur auteur, Instant maintenant) {
		verifierFonctionnellementModifiable();
		Objects.requireNonNull(auteur, "auteur obligatoire");
		Objects.requireNonNull(maintenant, "instant obligatoire");

		if (statut != StatutDemande.EN_COURS) {
			throw new TransitionInvalidException(
					"La résolution exige l'état EN_COURS, état actuel : " + statut + ".");
		}
		if (solution == null || solution.isBlank()) {
			throw new SolutionRequiseException();
		}
		statut = StatutDemande.RESOLUE;
		dateResolution = maintenant;
		dateModification = maintenant;
		ajouterEvenement(maintenant, EvenementsDemande.RESOLUTION,
				StatutDemande.EN_COURS.name(), StatutDemande.RESOLUE.name(), auteur);
	}

	/**
	 * Refus de résolution (RM38) : {@code RESOLUE → EN_COURS}. La description du
	 * traitement et la solution sont conservées ; {@code dateResolution} repasse
	 * à {@code null}. L'événement {@code RESOLUTION} antérieur conserve la
	 * traçabilité de l'ancienne résolution.
	 */
	public void refuserResolution(Utilisateur auteur, Instant maintenant) {
		verifierFonctionnellementModifiable();
		Objects.requireNonNull(auteur, "auteur obligatoire");
		Objects.requireNonNull(maintenant, "instant obligatoire");

		if (statut != StatutDemande.RESOLUE) {
			throw new TransitionInvalidException(
					"Le refus de résolution exige l'état RESOLUE, état actuel : " + statut + ".");
		}
		statut = StatutDemande.EN_COURS;
		dateResolution = null;
		dateModification = maintenant;
		ajouterEvenement(maintenant, EvenementsDemande.REFUS_RESOLUTION,
				StatutDemande.RESOLUE.name(), StatutDemande.EN_COURS.name(), auteur);
	}

	/**
	 * Clôture (RM37) : {@code RESOLUE → CLOTUREE}, état terminal.
	 */
	public void cloturer(Utilisateur auteur, Instant maintenant) {
		verifierFonctionnellementModifiable();
		Objects.requireNonNull(auteur, "auteur obligatoire");
		Objects.requireNonNull(maintenant, "instant obligatoire");

		if (statut != StatutDemande.RESOLUE) {
			throw new TransitionInvalidException(
					"La clôture exige l'état RESOLUE, état actuel : " + statut + ".");
		}
		statut = StatutDemande.CLOTUREE;
		dateCloture = maintenant;
		dateModification = maintenant;
		ajouterEvenement(maintenant, EvenementsDemande.CLOTURE,
				StatutDemande.RESOLUE.name(), StatutDemande.CLOTUREE.name(), auteur);
	}

	/**
	 * Annulation (RM40, RM41, RM42) : autorisée uniquement depuis
	 * {@code NOUVELLE}, {@code ASSIGNEE} ou {@code EN_COURS} ; interdite depuis
	 * {@code RESOLUE} et depuis les états terminaux. Motif obligatoire non blanc.
	 */
	public void annuler(String motif, Utilisateur auteur, Instant maintenant) {
		verifierFonctionnellementModifiable();
		if (motif == null || motif.isBlank()) {
			throw new IllegalArgumentException("motif obligatoire non blanc");
		}
		Objects.requireNonNull(auteur, "auteur obligatoire");
		Objects.requireNonNull(maintenant, "instant obligatoire");

		if (statut != StatutDemande.NOUVELLE && statut != StatutDemande.ASSIGNEE && statut != StatutDemande.EN_COURS) {
			throw new TransitionInvalidException(
					"Annulation interdite depuis l'état " + statut + ".");
		}
		StatutDemande avant = statut;
		statut = StatutDemande.ANNULEE;
		motifAnnulation = motif;
		dateAnnulation = maintenant;
		dateModification = maintenant;
		ajouterEvenement(maintenant, EvenementsDemande.ANNULATION, avant.name(), motif, auteur);
	}

	/**
	 * Ajoute un événement de traçabilité à la demande et maintient l'association
	 * des deux côtés. Aucune suppression ni réécriture d'événement existant :
	 * une correction produit un nouvel événement.
	 */
	public HistoriqueDemande ajouterEvenement(
			Instant dateEvenement,
			String typeEvenement,
			String ancienneValeur,
			String nouvelleValeur,
			Utilisateur auteur) {
		HistoriqueDemande evenement = new HistoriqueDemande(
				dateEvenement, typeEvenement, ancienneValeur, nouvelleValeur, auteur, this);
		this.historique.add(evenement);
		return evenement;
	}

	public Long getId() {
		return id;
	}

	public String getReference() {
		return reference;
	}

	public String getTitre() {
		return titre;
	}

	public String getDescription() {
		return description;
	}

	public Categorie getCategorie() {
		return categorie;
	}

	public Priorite getPriorite() {
		return priorite;
	}

	public StatutDemande getStatut() {
		return statut;
	}

	public String getDescriptionTraitement() {
		return descriptionTraitement;
	}

	public String getSolution() {
		return solution;
	}

	public String getMotifAnnulation() {
		return motifAnnulation;
	}

	public Instant getDateCreation() {
		return dateCreation;
	}

	public Instant getDateModification() {
		return dateModification;
	}

	public Instant getDateResolution() {
		return dateResolution;
	}

	public Instant getDateCloture() {
		return dateCloture;
	}

	public Instant getDateAnnulation() {
		return dateAnnulation;
	}

	public Client getClient() {
		return client;
	}

	public Utilisateur getCreateur() {
		return createur;
	}

	public Utilisateur getAgentAffecte() {
		return agentAffecte;
	}

	/**
	 * Retourne une vue non modifiable de l'historique : aucun événement existant
	 * ne peut être retiré ou réécrit depuis l'extérieur.
	 */
	public List<HistoriqueDemande> getHistorique() {
		return List.copyOf(historique);
	}
}
