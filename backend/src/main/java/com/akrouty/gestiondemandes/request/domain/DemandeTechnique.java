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

	protected DemandeTechnique() {
	}

	public DemandeTechnique(
			String reference,
			String titre,
			String description,
			Categorie categorie,
			Priorite priorite,
			StatutDemande statut,
			Client client,
			Utilisateur createur,
			Instant dateCreation,
			Instant dateModification) {
		this.reference = Objects.requireNonNull(reference, "reference obligatoire");
		this.titre = Objects.requireNonNull(titre, "titre obligatoire");
		this.description = Objects.requireNonNull(description, "description obligatoire");
		this.categorie = Objects.requireNonNull(categorie, "categorie obligatoire");
		this.priorite = Objects.requireNonNull(priorite, "priorite obligatoire");
		this.statut = Objects.requireNonNull(statut, "statut obligatoire");
		this.client = Objects.requireNonNull(client, "client obligatoire");
		this.createur = Objects.requireNonNull(createur, "createur obligatoire");
		this.dateCreation = Objects.requireNonNull(dateCreation, "dateCreation obligatoire");
		this.dateModification = Objects.requireNonNull(dateModification, "dateModification obligatoire");
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
