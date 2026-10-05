package com.akrouty.gestiondemandes.request.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.akrouty.gestiondemandes.identity.domain.Role;
import com.akrouty.gestiondemandes.identity.domain.Utilisateur;
import com.akrouty.gestiondemandes.identity.persistence.UtilisateurRepository;
import com.akrouty.gestiondemandes.request.domain.Categorie;
import com.akrouty.gestiondemandes.request.domain.Client;
import com.akrouty.gestiondemandes.request.domain.DemandeTechnique;
import com.akrouty.gestiondemandes.request.domain.HistoriqueDemande;
import com.akrouty.gestiondemandes.request.domain.Priorite;
import com.akrouty.gestiondemandes.request.domain.StatutDemande;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests de persistance de l'historique via {@link DemandeTechnique}
 * sur le H2 de test isolé. Aucun repository dédié n'existe pour
 * {@link HistoriqueDemande} (ADR-003).
 */
@SpringBootTest
@Transactional
class HistoriqueDemandeTest {

	private static final String HASH_DE_TEST = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

	@Autowired
	private DemandeTechniqueRepository demandeRepository;

	@Autowired
	private UtilisateurRepository utilisateurRepository;

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	private EntityManager entityManager;

	private Client client;
	private Utilisateur createur;

	@BeforeEach
	void creerClientEtCreateur() {
		client = clientRepository.save(new Client("Client Test", "client@example.com", "+216 71 123 456"));
		createur = utilisateurRepository.save(new Utilisateur(
				"Responsable Test",
				"responsable@example.com",
				true,
				HASH_DE_TEST,
				Set.of(Role.RESPONSABLE_TECHNIQUE)));
		entityManager.flush();
	}

	private DemandeTechnique nouvelleDemande(String reference, Instant creation) {
		return new DemandeTechnique(
				reference,
				"Titre de test",
				"Description de test",
				Categorie.NOTE_CALCUL,
				Priorite.MOYENNE,
				StatutDemande.NOUVELLE,
				client,
				createur,
				creation,
				creation);
	}

	@Test
	void creation_de_demande_avec_historique_utilise_la_cascade_persist() {
		Instant creation = Instant.now().truncatedTo(ChronoUnit.MICROS);
		DemandeTechnique demande = nouvelleDemande("REF-TEST-H001", creation);
		demande.ajouterEvenement(creation, "CREATION", null, "NOUVELLE", createur);

		// Seule la demande est enregistrée : cascade PERSIST uniquement.
		demandeRepository.save(demande);
		Long id = demande.getId();
		entityManager.flush();
		entityManager.clear();

		DemandeTechnique relu = demandeRepository.findById(id).orElseThrow();
		assertThat(relu.getHistorique()).hasSize(1);
		HistoriqueDemande evenement = relu.getHistorique().getFirst();
		assertThat(evenement.getDateEvenement()).isEqualTo(creation);
		assertThat(evenement.getTypeEvenement()).isEqualTo("CREATION");
		assertThat(evenement.getAncienneValeur()).isNull();
		assertThat(evenement.getNouvelleValeur()).isEqualTo("NOUVELLE");
		assertThat(evenement.getDemande().getId()).isEqualTo(id);
		assertThat(evenement.getAuteur().getId()).isEqualTo(createur.getId());
		assertThat(evenement.getAuteur().getEmail()).isEqualTo("responsable@example.com");
	}

	@Test
	void auteur_et_date_evenement_sont_obligatoires() {
		Instant maintenant = Instant.now().truncatedTo(ChronoUnit.MICROS);

		assertThatThrownBy(() -> new HistoriqueDemande(maintenant, "CREATION", null, null, null))
				.isInstanceOf(NullPointerException.class)
				.hasMessageContaining("auteur");

		assertThatThrownBy(() -> new HistoriqueDemande(null, "CREATION", null, null, createur))
				.isInstanceOf(NullPointerException.class)
				.hasMessageContaining("dateEvenement");
	}

	@Test
	void evenement_ajoute_apres_creation_est_relue_avec_ses_valeurs() {
		Instant creation = Instant.now().truncatedTo(ChronoUnit.MICROS);
		DemandeTechnique demande = nouvelleDemande("REF-TEST-H002", creation);
		demandeRepository.saveAndFlush(demande);

		Instant modification = creation.plusSeconds(120);
		demande.ajouterEvenement(modification, "MODIFICATION", "MOYENNE", "HAUTE", createur);
		entityManager.flush();
		Long id = demande.getId();
		entityManager.clear();

		DemandeTechnique relu = demandeRepository.findById(id).orElseThrow();
		assertThat(relu.getHistorique()).hasSize(1);
		HistoriqueDemande evenement = relu.getHistorique().getFirst();
		assertThat(evenement.getAncienneValeur()).isEqualTo("MOYENNE");
		assertThat(evenement.getNouvelleValeur()).isEqualTo("HAUTE");
		assertThat(evenement.getAuteur().getId()).isEqualTo(createur.getId());
	}
}
