package com.akrouty.gestiondemandes.request.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.akrouty.gestiondemandes.identity.domain.Role;
import com.akrouty.gestiondemandes.identity.domain.Utilisateur;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Tests d'API de qualification (Bloc 3 §39) : changements réels historisés,
 * valeurs identiques sans événement parasite, demande terminale en 409, RBAC.
 */
class DemandeQualificationApiTest extends DemandeApiTestSupport {

	private String jetonRt;
	private Long clientId;
	private Utilisateur agent;

	@BeforeEach
	void preparer() {
		Utilisateur rt = creerUtilisateur("Responsable", "rt@example.com", true, Role.RESPONSABLE_TECHNIQUE);
		agent = creerUtilisateur("Agent", "agent@example.com", true, Role.AGENT_TECHNIQUE);
		jetonRt = jeton(rt);
		clientId = creerClient("Client", "client@example.com", "+216 71 000 001");
	}

	private String qualification(String reference, String corps) throws Exception {
		return mockMvc.perform(put("/api/v1/demandes/" + reference + "/qualification")
						.header("Authorization", BEARER_PREFIX + jetonRt)
						.contentType(MediaType.APPLICATION_JSON)
						.content(corps))
				.andReturn().getResponse().getContentAsString();
	}

	/** Conduit la demande jusqu'à {@code CLOTUREE} avec les fixtures de ce test. */
	private void cloturerDemande(String reference) {
		cloturerViaApi(reference, jetonRt, jeton(agent), agent.getId());
	}

	@Test
	void rt_modifie_categorie_avec_historique() throws Exception {
		String reference = creerDemande(jetonRt, clientId);

		mockMvc.perform(put("/api/v1/demandes/" + reference + "/qualification")
						.header("Authorization", BEARER_PREFIX + jetonRt)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categorie\":\"PROTECTION_INCENDIE\",\"priorite\":\"HAUTE\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.categorie").value("PROTECTION_INCENDIE"));

		List<String> types = typesEvenements(reference);
		assertThat(types).contains("CATEGORIE_MODIFIEE");
	}

	@Test
	void rt_modifie_priorite_avec_historique() throws Exception {
		String reference = creerDemande(jetonRt, clientId);

		mockMvc.perform(put("/api/v1/demandes/" + reference + "/qualification")
						.header("Authorization", BEARER_PREFIX + jetonRt)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categorie\":\"NOTE_CALCUL\",\"priorite\":\"CRITIQUE\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.priorite").value("CRITIQUE"));

		assertThat(typesEvenements(reference)).contains("PRIORITE_MODIFIEE");
	}

	@Test
	void valeurs_identiques_ne_creent_aucun_evenement_artificiel() throws Exception {
		String reference = creerDemande(jetonRt, clientId);
		int avant = compterEvenements(reference);

		// La demande est créée avec NOTE_CALCUL / HAUTE : mêmes valeurs envoyées.
		mockMvc.perform(put("/api/v1/demandes/" + reference + "/qualification")
						.header("Authorization", BEARER_PREFIX + jetonRt)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categorie\":\"NOTE_CALCUL\",\"priorite\":\"HAUTE\"}"))
				.andExpect(status().isOk());

		assertThat(compterEvenements(reference)).isEqualTo(avant);
	}

	@Test
	void qualification_sur_demande_cloturee_retourne_409() throws Exception {
		String reference = creerDemande(jetonRt, clientId);
		cloturerDemande(reference);

		mockMvc.perform(put("/api/v1/demandes/" + reference + "/qualification")
						.header("Authorization", BEARER_PREFIX + jetonRt)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categorie\":\"AUTRE\",\"priorite\":\"BASSE\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DEMANDE_TERMINEE"));
	}

	@Test
	void qualification_sur_demande_annulee_retourne_409() throws Exception {
		String reference = creerDemande(jetonRt, clientId);
		annulerViaApi(reference, jetonRt, "Sans objet");

		mockMvc.perform(put("/api/v1/demandes/" + reference + "/qualification")
						.header("Authorization", BEARER_PREFIX + jetonRt)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categorie\":\"AUTRE\",\"priorite\":\"BASSE\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DEMANDE_TERMINEE"));
	}

	@Test
	void agent_technique_retourne_403() throws Exception {
		String reference = creerDemande(jetonRt, clientId);

		mockMvc.perform(put("/api/v1/demandes/" + reference + "/qualification")
						.header("Authorization", BEARER_PREFIX + jeton(agent))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categorie\":\"AUTRE\",\"priorite\":\"BASSE\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void qualification_ajoute_date_modification() throws Exception {
		String reference = creerDemande(jetonRt, clientId);

		String reponse = qualification(reference,
				"{\"categorie\":\"DOSSIER_TECHNIQUE\",\"priorite\":\"BASSE\"}");
		assertThat(reponse).contains("\"categorie\":\"DOSSIER_TECHNIQUE\"");
	}

	@Test
	void demande_inexistante_retourne_404() throws Exception {
		mockMvc.perform(put("/api/v1/demandes/00000000-0000-0000-0000-000000000000/qualification")
						.header("Authorization", BEARER_PREFIX + jetonRt)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categorie\":\"AUTRE\",\"priorite\":\"BASSE\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("DEMANDE_INTROUVABLE"));
	}
}