package com.akrouty.gestiondemandes.security.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.akrouty.gestiondemandes.identity.domain.Role;
import com.akrouty.gestiondemandes.identity.domain.Utilisateur;
import com.akrouty.gestiondemandes.support.ApiTestSupport;
import org.junit.jupiter.api.Test;

/**
 * CORS (SECURITY-DESIGN-V1 §12) : seules les origines de
 * {@code CORS_ALLOWED_ORIGINS} sont autorisées (TEST : http://localhost:4200).
 * Aucun wildcard, aucune ouverture par défaut.
 */
class CorsApiTest extends ApiTestSupport {

	/** Origine TEST configurée dans {@code src/test/resources/application.properties}. */
	private static final String ORIGINE_TEST = "http://localhost:4200";

	@Test
	void preflight_depuis_origine_autorisee_recit_les_headers_cors() throws Exception {
		mockMvc.perform(options("/api/v1/utilisateurs")
						.header("Origin", ORIGINE_TEST)
						.header("Access-Control-Request-Method", "GET")
						.header("Access-Control-Request-Headers", "Authorization"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", ORIGINE_TEST))
				.andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("GET")))
				.andExpect(header().string("Access-Control-Allow-Headers", org.hamcrest.Matchers.containsString("Authorization")));
	}

	@Test
	void requete_depuis_origine_non_autorisee_n_obtient_aucune_autorisation_cors() throws Exception {
		mockMvc.perform(options("/api/v1/utilisateurs")
						.header("Origin", "http://evil.example")
						.header("Access-Control-Request-Method", "GET")
						.header("Access-Control-Request-Headers", "Authorization"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
	}

	@Test
	void requete_reelle_depuis_origine_autorisee_recit_le_header_allow_origin() throws Exception {
		Utilisateur admin = creerUtilisateur("Admin", "admin@example.com", true, Role.ADMINISTRATEUR);

		mockMvc.perform(get("/api/v1/utilisateurs")
						.header("Origin", ORIGINE_TEST)
						.header("Authorization", BEARER_PREFIX + jeton(admin)))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", ORIGINE_TEST));
	}
}