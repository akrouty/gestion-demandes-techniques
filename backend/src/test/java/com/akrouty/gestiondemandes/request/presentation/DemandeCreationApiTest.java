package com.akrouty.gestiondemandes.request.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.akrouty.gestiondemandes.identity.domain.Role;
import com.akrouty.gestiondemandes.identity.domain.Utilisateur;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Tests d'API de création d'une demande (Bloc 3 §38) : client existant ou
 * nouveau, référence serveur, statut initial, Location, contrôle exactement-un,
 * erreurs et RBAC.
 */
class DemandeCreationApiTest extends DemandeApiTestSupport {

	private Utilisateur rt;
	private Utilisateur agent;
	private Utilisateur admin;
	private Long clientId;
	private String jetonRt;

	@BeforeEach
	void preparer() {
		rt = creerUtilisateur("Responsable", "rt@example.com", true, Role.RESPONSABLE_TECHNIQUE);
		agent = creerUtilisateur("Agent", "agent@example.com", true, Role.AGENT_TECHNIQUE);
		admin = creerUtilisateur("Admin", "admin@example.com", true, Role.ADMINISTRATEUR);
		clientId = creerClient("Client Existant", "existant@example.com", "+216 71 000 000");
		jetonRt = jeton(rt);
	}

	@Test
	void rt_client_existant_cree_une_demande_201_avec_location() throws Exception {
		mockMvc.perform(post("/api/v1/demandes")
						.header("Authorization", BEARER_PREFIX + jetonRt)
						.contentType(MediaType.APPLICATION_JSON)
						.content(corpsCreation(clientId)))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location",
						org.hamcrest.Matchers.matchesPattern("/api/v1/demandes/[0-9a-f\\-]{36}")))
				.andExpect(jsonPath("$.statut").value("NOUVELLE"))
				.andExpect(jsonPath("$.titre").value("Panne moteur"))
				.andExpect(jsonPath("$.categorie").value("NOTE_CALCUL"))
				.andExpect(jsonPath("$.priorite").value("HAUTE"))
				.andExpect(jsonPath("$.client.id").value(clientId))
				.andExpect(jsonPath("$.createur.id").value(rt.getId()))
				.andExpect(jsonPath("$.agentAffecte").doesNotExist())
				.andExpect(jsonPath("$.dateCreation").isNotEmpty())
				.andExpect(jsonPath("$.dateModification").isNotEmpty())
				.andExpect(jsonPath("$.dateResolution").doesNotExist())
				.andExpect(jsonPath("$.dateCloture").doesNotExist())
				.andExpect(jsonPath("$.dateAnnulation").doesNotExist());
	}

	@Test
	void rt_nouveau_client_cree_demande_et_client_persiste() throws Exception {
		long clientsAvant = clientRepository.count();

		mockMvc.perform(post("/api/v1/demandes")
						.header("Authorization", BEARER_PREFIX + jetonRt)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"titre":"Nouveau besoin","description":"Description nouvelle",
								 "categorie":"AUTRE","priorite":"BASSE",
								 "nouveauClient":{"nom":"Client Neuf","email":"neuf@example.com",
								 "telephone":"+216 22 333 444"}}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.statut").value("NOUVELLE"))
				.andExpect(jsonPath("$.client.nom").value("Client Neuf"));

		// RM08 : le nouveau client est persisté et réutilisable.
		assertThat(clientRepository.count()).isEqualTo(clientsAvant + 1);
		assertThat(clientRepository.findAll())
				.extracting(c -> c.getEmail())
				.contains("neuf@example.com");
	}

	@Test
	void reference_est_generervee_serveur_unique_et_distincte_de_id() throws Exception {
		String ref1 = creerDemande(jetonRt, clientId);
		String ref2 = creerDemande(jetonRt, clientId);

		assertThat(ref1).isNotBlank();
		assertThat(ref1).matches("[0-9a-f\\-]{36}");
		assertThat(ref1).isNotEqualTo(ref2);

		// La référence est distincte de l'id technique et générée côté serveur.
		var demande = demandeRepository.findByReference(ref1).orElseThrow();
		assertThat(demande.getReference()).isNotEqualTo(String.valueOf(demande.getId()));

		// Aucun DTO d'entrée ne permet d'imposer référence/statut/createur/dates.
		mockMvc.perform(post("/api/v1/demandes")
						.header("Authorization", BEARER_PREFIX + jetonRt)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"titre":"Forcé","description":"Tentative de controle client",
								 "categorie":"AUTRE","priorite":"BASSE","clientId":%d,
								 "reference":"00000000-0000-0000-0000-000000000001",
								 "statut":"CLOTUREE","createur":{"id":999},
								 "dateCreation":"2000-01-01T00:00:00Z"}
								""".formatted(clientId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.statut").value("NOUVELLE"))
				.andExpect(jsonPath("$.reference")
						.value(org.hamcrest.Matchers.not("00000000-0000-0000-0000-000000000001")))
				.andExpect(jsonPath("$.createur.id").value(rt.getId()));
	}

	@Test
	void statut_initial_est_nouvelle_et_createur_est_le_rt_connecte() throws Exception {
		String reference = creerDemande(jetonRt, clientId);

		var demande = demandeRepository.findByReference(reference).orElseThrow();
		assertThat(demande.getStatut()).isEqualTo(com.akrouty.gestiondemandes.request.domain.StatutDemande.NOUVELLE);
		assertThat(demande.getCreateur().getId()).isEqualTo(rt.getId());
		// Dates serveur : dateCreation = dateModification à la création.
		assertThat(demande.getDateCreation()).isEqualTo(demande.getDateModification());
	}
	// ---------------------------------------------------------------- contrôles d'entrée & RBAC

	@Test
	void clientId_et_nouveauClient_ensemble_retournent_400() throws Exception {
		mockMvc.perform(post("/api/v1/demandes")
						.header("Authorization", BEARER_PREFIX + jetonRt)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"titre":"Double","description":"Les deux fournis",
								 "categorie":"AUTRE","priorite":"BASSE","clientId":%d,
								 "nouveauClient":{"nom":"N","email":"n@example.com","telephone":"1"}}
								""".formatted(clientId)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void sans_client_ni_nouveauClient_retourne_400() throws Exception {
		mockMvc.perform(post("/api/v1/demandes")
						.header("Authorization", BEARER_PREFIX + jetonRt)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"titre":"Aucun client","description":"Oubli du client",
								 "categorie":"AUTRE","priorite":"BASSE"}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void client_inexistant_retourne_404() throws Exception {
		mockMvc.perform(post("/api/v1/demandes")
						.header("Authorization", BEARER_PREFIX + jetonRt)
						.contentType(MediaType.APPLICATION_JSON)
						.content(corpsCreation(999_999L)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("CLIENT_INTROUVABLE"));
	}

	@Test
	void champs_obligatoires_absents_retournent_400() throws Exception {
		mockMvc.perform(post("/api/v1/demandes")
						.header("Authorization", BEARER_PREFIX + jetonRt)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"clientId\":" + clientId + "}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION"));
	}

	@Test
	void nouveau_client_avec_email_invalide_retourne_400() throws Exception {
		mockMvc.perform(post("/api/v1/demandes")
						.header("Authorization", BEARER_PREFIX + jetonRt)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"titre":"T","description":"D","categorie":"AUTRE","priorite":"BASSE",
								 "nouveauClient":{"nom":"N","email":"pas-un-email","telephone":"1"}}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void agent_seul_retourne_403() throws Exception {
		mockMvc.perform(post("/api/v1/demandes")
						.header("Authorization", BEARER_PREFIX + jeton(agent))
						.contentType(MediaType.APPLICATION_JSON)
						.content(corpsCreation(clientId)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCES_INTERDIT"));
	}

	@Test
	void administrateur_seul_retourne_403() throws Exception {
		mockMvc.perform(post("/api/v1/demandes")
						.header("Authorization", BEARER_PREFIX + jeton(admin))
						.contentType(MediaType.APPLICATION_JSON)
						.content(corpsCreation(clientId)))
				.andExpect(status().isForbidden());
	}

	@Test
	void sans_jwt_retourne_401() throws Exception {
		mockMvc.perform(post("/api/v1/demandes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(corpsCreation(clientId)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void creation_produit_un_evenement_historique_de_creation() throws Exception {
		String reference = creerDemande(jetonRt, clientId);

		List<String> types = typesEvenements(reference);
		assertThat(types).containsExactly("CREATION");
	}

	@Test
	void les_demandes_sont_lues_avec_le_client_et_le_createur() throws Exception {
		String reference = creerDemande(jetonRt, clientId);

		mockMvc.perform(get("/api/v1/demandes/" + reference)
						.header("Authorization", BEARER_PREFIX + jetonRt))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reference").value(reference))
				.andExpect(jsonPath("$.client.nom").value("Client Existant"))
				.andExpect(jsonPath("$.createur.email").value("rt@example.com"))
				// L'historique complet n'est jamais inclus dans le détail.
				.andExpect(jsonPath("$.historique").doesNotExist());
	}
}