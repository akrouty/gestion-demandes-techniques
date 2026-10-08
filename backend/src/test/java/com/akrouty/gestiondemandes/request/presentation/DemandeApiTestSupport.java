package com.akrouty.gestiondemandes.request.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.akrouty.gestiondemandes.request.domain.Client;
import com.akrouty.gestiondemandes.request.persistence.ClientRepository;
import com.akrouty.gestiondemandes.request.persistence.DemandeTechniqueRepository;
import com.akrouty.gestiondemandes.support.ApiTestSupport;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Support des tests d'API des demandes (Bloc 3) : fixtures et helpers de
 * conduite du cycle de vie via l'API réelle (aucun raccourci de contournement
 * de la sécurité).
 */
abstract class DemandeApiTestSupport extends ApiTestSupport {

	@Autowired
	protected ClientRepository clientRepository;

	@Autowired
	protected DemandeTechniqueRepository demandeRepository;

	protected Long creerClient(String nom, String email, String telephone) {
		return clientRepository.save(new Client(nom, email, telephone)).getId();
	}

	protected String corpsCreation(Long clientId) {
		return """
				{"titre":"Panne moteur","description":"Le moteur ne démarre plus",
				 "categorie":"NOTE_CALCUL","priorite":"HAUTE","clientId":%d}
				""".formatted(clientId);
	}

	/** Crée une demande via l'API et retourne sa référence (Location). */
	protected String creerDemande(String jetonRt, Long clientId) {
		try {
			MvcResult resultat = mockMvc.perform(post("/api/v1/demandes")
							.header("Authorization", BEARER_PREFIX + jetonRt)
							.contentType(MediaType.APPLICATION_JSON)
							.content(corpsCreation(clientId)))
					.andExpect(status().isCreated())
					.andReturn();
			return JsonPath.read(corps(resultat), "$.reference");
		} catch (Exception e) {
			throw new IllegalStateException("Création de demande de test impossible", e);
		}
	}

	protected void affecter(String jetonRt, String reference, Long agentId) {
		try {
			mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
							.put("/api/v1/demandes/" + reference + "/affectation")
							.header("Authorization", BEARER_PREFIX + jetonRt)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"agentId\":" + agentId + "}"))
					.andExpect(status().isOk());
		} catch (Exception e) {
			throw new IllegalStateException("Affectation de test impossible", e);
		}
	}

	protected void demarrerTraitement(String jetonAgent, String reference) {
		try {
			mockMvc.perform(post("/api/v1/demandes/" + reference + "/demarrage-traitement")
							.header("Authorization", BEARER_PREFIX + jetonAgent))
					.andExpect(status().isOk());
		} catch (Exception e) {
			throw new IllegalStateException("Démarrage de test impossible", e);
		}
	}

	protected void majTraitement(String jetonAgent, String reference, String corpsJson) {
		try {
			mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
							.patch("/api/v1/demandes/" + reference + "/traitement")
							.header("Authorization", BEARER_PREFIX + jetonAgent)
							.contentType(MediaType.APPLICATION_JSON)
							.content(corpsJson))
					.andExpect(status().isOk());
		} catch (Exception e) {
			throw new IllegalStateException("Mise à jour de traitement de test impossible", e);
		}
	}

	protected void resoudre(String jetonAgent, String reference) {
		try {
			mockMvc.perform(post("/api/v1/demandes/" + reference + "/resolution")
							.header("Authorization", BEARER_PREFIX + jetonAgent))
					.andExpect(status().isOk());
		} catch (Exception e) {
			throw new IllegalStateException("Résolution de test impossible", e);
		}
	}

	/**
	 * Conduit une demande jusqu'à l'état {@code CLOTUREE} :
	 * affectation → démarrage → solution → résolution → clôture.
	 */
	protected void cloturerViaApi(String reference, String jetonRt_, String jetonAgent_, Long agentId_) {
		affecter(jetonRt_, reference, agentId_);
		demarrerTraitement(jetonAgent_, reference);
		majTraitement(jetonAgent_, reference, "{\"solution\":\"Solution appliquée\"}");
		resoudre(jetonAgent_, reference);
		cloturer(jetonRt_, reference);
	}

	protected void cloturer(String jetonRt_, String reference) {
		try {
			mockMvc.perform(post("/api/v1/demandes/" + reference + "/cloture")
							.header("Authorization", BEARER_PREFIX + jetonRt_))
					.andExpect(status().isOk());
		} catch (Exception e) {
			throw new IllegalStateException("Clôture de test impossible", e);
		}
	}

	protected void annulerViaApi(String reference, String jetonRt_, String motif) {
		try {
			mockMvc.perform(post("/api/v1/demandes/" + reference + "/annulation")
							.header("Authorization", BEARER_PREFIX + jetonRt_)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"motif\":\"" + motif + "\"}"))
					.andExpect(status().isOk());
		} catch (Exception e) {
			throw new IllegalStateException("Annulation de test impossible", e);
		}
	}

	/** Compte les événements d'historique d'une demande (lecture SQL directe). */
	protected int compterEvenements(String reference) {
		Integer compte = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM historique_demande h "
						+ "JOIN demande_technique d ON h.demande_id = d.id WHERE d.reference = ?",
				Integer.class, reference);
		return compte == null ? 0 : compte;
	}

	/** Types des événements d'historique dans l'ordre d'insertion. */
	protected List<String> typesEvenements(String reference) {
		return jdbcTemplate.queryForList(
				"SELECT h.type_evenement FROM historique_demande h "
						+ "JOIN demande_technique d ON h.demande_id = d.id "
						+ "WHERE d.reference = ? ORDER BY h.id",
				String.class, reference);
	}
}
