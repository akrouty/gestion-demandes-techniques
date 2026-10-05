package com.akrouty.gestiondemandes.request.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.akrouty.gestiondemandes.identity.domain.Role;
import com.akrouty.gestiondemandes.identity.domain.Utilisateur;
import com.akrouty.gestiondemandes.identity.persistence.UtilisateurRepository;
import com.akrouty.gestiondemandes.request.domain.Categorie;
import com.akrouty.gestiondemandes.request.domain.Client;
import com.akrouty.gestiondemandes.request.domain.DemandeTechnique;
import com.akrouty.gestiondemandes.request.domain.Priorite;
import com.akrouty.gestiondemandes.request.domain.StatutDemande;
import jakarta.persistence.EntityManager;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests de persistance de {@link DemandeTechnique} sur le H2 de test isolé.
 *
 * <p>Les références utilisées ici sont des données de test contrôlées :
 * le format métier de la référence n'est pas validé et sa génération
 * réelle viendra avec le cas d'utilisation de création.</p>
 */
@SpringBootTest
@Transactional
class DemandeTechniqueRepositoryTest {

	private static final String HASH_DE_TEST = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

	@Autowired
	private DemandeTechniqueRepository demandeRepository;

	@Autowired
	private UtilisateurRepository utilisateurRepository;

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

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
				Categorie.PROTECTION_INCENDIE,
				Priorite.HAUTE,
				StatutDemande.NOUVELLE,
				client,
				createur,
				creation,
				creation.plusSeconds(60));
	}

	@Test
	void demande_est_persistee_et_relue_avec_relations_enums_et_instants() {
		Instant creation = Instant.now().truncatedTo(ChronoUnit.MICROS);
		DemandeTechnique demande = nouvelleDemande("REF-TEST-001", creation);
		demandeRepository.save(demande);
		Long id = demande.getId();
		entityManager.flush();
		entityManager.clear();

		DemandeTechnique relu = demandeRepository.findById(id).orElseThrow();
		assertThat(relu.getReference()).isEqualTo("REF-TEST-001");
		assertThat(relu.getTitre()).isEqualTo("Titre de test");
		assertThat(relu.getDescription()).isEqualTo("Description de test");
		assertThat(relu.getCategorie()).isEqualTo(Categorie.PROTECTION_INCENDIE);
		assertThat(relu.getPriorite()).isEqualTo(Priorite.HAUTE);
		assertThat(relu.getStatut()).isEqualTo(StatutDemande.NOUVELLE);
		assertThat(relu.getDateCreation()).isEqualTo(creation);
		assertThat(relu.getDateModification()).isEqualTo(creation.plusSeconds(60));
		assertThat(relu.getDateResolution()).isNull();
		assertThat(relu.getDateCloture()).isNull();
		assertThat(relu.getDateAnnulation()).isNull();
		assertThat(relu.getDescriptionTraitement()).isNull();
		assertThat(relu.getSolution()).isNull();
		assertThat(relu.getMotifAnnulation()).isNull();

		// agentAffecte est optionnel
		assertThat(relu.getAgentAffecte()).isNull();

		// associations nécessaires persistées et relues
		assertThat(relu.getClient().getId()).isEqualTo(client.getId());
		assertThat(relu.getClient().getNom()).isEqualTo("Client Test");
		assertThat(relu.getCreateur().getId()).isEqualTo(createur.getId());
		assertThat(relu.getCreateur().getEmail()).isEqualTo("responsable@example.com");
		assertThat(relu.getHistorique()).isEmpty();

		// enums persistés comme codes textuels
		assertThat(jdbcTemplate.queryForObject(
				"SELECT categorie FROM demande_technique WHERE id = ?", String.class, id))
				.isEqualTo("PROTECTION_INCENDIE");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT priorite FROM demande_technique WHERE id = ?", String.class, id))
				.isEqualTo("HAUTE");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT statut FROM demande_technique WHERE id = ?", String.class, id))
				.isEqualTo("NOUVELLE");
	}

	@Test
	void agent_affecte_optionnel_peut_etre_persiste_et_relue() throws Exception {
		Utilisateur agent = utilisateurRepository.save(new Utilisateur(
				"Agent Test",
				"agent@example.com",
				true,
				HASH_DE_TEST,
				Set.of(Role.AGENT_TECHNIQUE)));
		Instant creation = Instant.now().truncatedTo(ChronoUnit.MICROS);
		DemandeTechnique demande = nouvelleDemande("REF-TEST-002", creation);

		// L'affectation sera portée par le cas d'utilisation métier (bloc suivant) :
		// aucun setter n'est introduit ici, le mapping est testé au niveau persistance.
		Field agentAffecte = DemandeTechnique.class.getDeclaredField("agentAffecte");
		agentAffecte.setAccessible(true);
		agentAffecte.set(demande, agent);

		demandeRepository.save(demande);
		Long id = demande.getId();
		entityManager.flush();
		entityManager.clear();

		DemandeTechnique relu = demandeRepository.findById(id).orElseThrow();
		assertThat(relu.getAgentAffecte()).isNotNull();
		assertThat(relu.getAgentAffecte().getId()).isEqualTo(agent.getId());
		assertThat(relu.getAgentAffecte().getEmail()).isEqualTo("agent@example.com");
	}

	@Test
	void reference_duplique_est_refusee() {
		Instant creation = Instant.now().truncatedTo(ChronoUnit.MICROS);
		demandeRepository.saveAndFlush(nouvelleDemande("REF-TEST-DUP", creation));

		DemandeTechnique doublon = nouvelleDemande("REF-TEST-DUP", creation.plusSeconds(1));
		assertThatThrownBy(() -> demandeRepository.saveAndFlush(doublon))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void reference_est_immuable_apres_insertion() throws Exception {
		Instant creation = Instant.now().truncatedTo(ChronoUnit.MICROS);
		DemandeTechnique demande = nouvelleDemande("REF-TEST-IMM", creation);
		demandeRepository.saveAndFlush(demande);
		Long id = demande.getId();

		// La conception ne fournit aucun setter public ; la colonne n'est pas updatable.
		assertThatThrownBy(() -> DemandeTechnique.class.getMethod("setReference", String.class))
				.isInstanceOf(NoSuchMethodException.class);

		Field reference = DemandeTechnique.class.getDeclaredField("reference");
		reference.setAccessible(true);
		reference.set(demande, "REF-TEST-MODIFIEE");
		entityManager.flush();
		entityManager.clear();

		DemandeTechnique relu = demandeRepository.findById(id).orElseThrow();
		assertThat(relu.getReference()).isEqualTo("REF-TEST-IMM");
	}
}
