package com.akrouty.gestiondemandes.request.domain;

import com.akrouty.gestiondemandes.identity.domain.Utilisateur;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

/**
 * Événement de traçabilité d'une demande.
 *
 * <p>Donnée de traçabilité fonctionnellement immuable : ni modification ni
 * suppression d'un événement existant, aucun setter public réécrivant son
 * contenu. Une correction produit un nouvel événement. Ceci n'est pas de
 * l'Event Sourcing.</p>
 */
@Entity
@Table(name = "historique_demande")
@SequenceGenerator(name = "historique_demande_seq", sequenceName = "historique_demande_seq", allocationSize = 1)
public class HistoriqueDemande {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "historique_demande_seq")
	private Long id;

	@Column(name = "date_evenement", nullable = false)
	private Instant dateEvenement;

	@Column(name = "type_evenement", nullable = false)
	private String typeEvenement;

	@Column(name = "ancienne_valeur")
	private String ancienneValeur;

	@Column(name = "nouvelle_valeur")
	private String nouvelleValeur;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "demande_id", nullable = false)
	private DemandeTechnique demande;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "auteur_id", nullable = false)
	private Utilisateur auteur;

	protected HistoriqueDemande() {
	}

	public HistoriqueDemande(
			Instant dateEvenement,
			String typeEvenement,
			String ancienneValeur,
			String nouvelleValeur,
			Utilisateur auteur) {
		this.dateEvenement = Objects.requireNonNull(dateEvenement, "dateEvenement obligatoire");
		this.typeEvenement = Objects.requireNonNull(typeEvenement, "typeEvenement obligatoire");
		this.ancienneValeur = ancienneValeur;
		this.nouvelleValeur = nouvelleValeur;
		this.auteur = Objects.requireNonNull(auteur, "auteur obligatoire");
	}

	/**
	 * Constructeur interne utilisé par {@link DemandeTechnique#ajouterEvenement}
	 * pour maintenir l'appartenance de l'événement à sa demande.
	 */
	HistoriqueDemande(
			Instant dateEvenement,
			String typeEvenement,
			String ancienneValeur,
			String nouvelleValeur,
			Utilisateur auteur,
			DemandeTechnique demande) {
		this(dateEvenement, typeEvenement, ancienneValeur, nouvelleValeur, auteur);
		this.demande = Objects.requireNonNull(demande, "demande obligatoire");
	}

	public Long getId() {
		return id;
	}

	public Instant getDateEvenement() {
		return dateEvenement;
	}

	public String getTypeEvenement() {
		return typeEvenement;
	}

	public String getAncienneValeur() {
		return ancienneValeur;
	}

	public String getNouvelleValeur() {
		return nouvelleValeur;
	}

	public DemandeTechnique getDemande() {
		return demande;
	}

	public Utilisateur getAuteur() {
		return auteur;
	}
}
