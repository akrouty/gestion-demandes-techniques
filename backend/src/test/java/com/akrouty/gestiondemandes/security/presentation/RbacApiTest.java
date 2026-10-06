package com.akrouty.gestiondemandes.security.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.akrouty.gestiondemandes.identity.domain.Role;
import com.akrouty.gestiondemandes.identity.domain.Utilisateur;
import com.akrouty.gestiondemandes.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * RBAC HTTP (SECURITY-DESIGN-V1 §5, API-CONTRACT-V1 §10).
 *
 * <p>Les routes métier n'ont pas encore de contrôleur : un accès refusé par
 * le RBAC produit {@code 403}, un accès autorisé atteint le DispatcherServlet
 * et produit {@code 404} — ce qui prouve la barrière sans créer de faux
 * contrôleur. Les contrôles contextuels (Agent affecté) restent au Bloc 3.</p>
 */
class RbacApiTest extends ApiTestSupport {

	@Test
	void sans_jwt_sur_route_protegee_401() throws Exception {
		mockMvc.perform(get("/api/v1/utilisateurs"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTIFICATION_REQUISE"));
	}

	@Test
	void administrateur_autorise_sur_administration() throws Exception {
		Utilisateur admin = creerUtilisateur("Admin", "admin@example.com", true, Role.ADMINISTRATEUR);

		mockMvc.perform(get("/api/v1/utilisateurs")
						.header("Authorization", BEARER_PREFIX + jeton(admin)))
				.andExpect(status().isOk());
	}

	@Test
	void responsable_technique_seul_refuse_sur_administration_403() throws Exception {
		Utilisateur rt = creerUtilisateur("RT", "rt@example.com", true, Role.RESPONSABLE_TECHNIQUE);

		mockMvc.perform(get("/api/v1/utilisateurs")
						.header("Authorization", BEARER_PREFIX + jeton(rt)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCES_INTERDIT"));
	}

	@Test
	void agent_technique_seul_refuse_sur_administration_403() throws Exception {
		Utilisateur at = creerUtilisateur("AT", "at@example.com", true, Role.AGENT_TECHNIQUE);

		mockMvc.perform(get("/api/v1/utilisateurs")
						.header("Authorization", BEARER_PREFIX + jeton(at)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCES_INTERDIT"));
	}

	@Test
	void administrateur_seul_refuse_sur_routes_metier_403() throws Exception {
		Utilisateur admin = creerUtilisateur("Admin", "admin@example.com", true, Role.ADMINISTRATEUR);
		String jeton = BEARER_PREFIX + jeton(admin);

		mockMvc.perform(post("/api/v1/demandes")
						.header("Authorization", jeton)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"titre\":\"t\",\"description\":\"d\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCES_INTERDIT"));

		mockMvc.perform(get("/api/v1/demandes").header("Authorization", jeton))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/v1/clients").header("Authorization", jeton))
				.andExpect(status().isForbidden());
	}

	@Test
	void responsable_technique_passe_la_barriere_des_routes_rt() throws Exception {
		Utilisateur rt = creerUtilisateur("RT", "rt@example.com", true, Role.RESPONSABLE_TECHNIQUE);
		String jeton = BEARER_PREFIX + jeton(rt);

		// Autorisé par le RBAC : 404 (contrôleur métier attendu au Bloc 3).
		mockMvc.perform(post("/api/v1/demandes")
						.header("Authorization", jeton)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"titre\":\"t\"}"))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/v1/clients").header("Authorization", jeton))
				.andExpect(status().isNotFound());
	}

	@Test
	void agent_technique_refuse_sur_routes_rt_et_autorise_sur_routes_at() throws Exception {
		Utilisateur at = creerUtilisateur("AT", "at@example.com", true, Role.AGENT_TECHNIQUE);
		String jeton = BEARER_PREFIX + jeton(at);

		mockMvc.perform(post("/api/v1/demandes")
						.header("Authorization", jeton)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"titre\":\"t\"}"))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/v1/clients").header("Authorization", jeton))
				.andExpect(status().isForbidden());

		// Autorisation sur les routes AT (404 = barrière franchie).
		mockMvc.perform(post("/api/v1/demandes/REF-1/demarrage-traitement")
						.header("Authorization", jeton))
				.andExpect(status().isNotFound());
	}

	@Test
	void responsable_technique_refuse_sur_route_at() throws Exception {
		Utilisateur rt = creerUtilisateur("RT", "rt@example.com", true, Role.RESPONSABLE_TECHNIQUE);

		mockMvc.perform(post("/api/v1/demandes/REF-1/demarrage-traitement")
						.header("Authorization", BEARER_PREFIX + jeton(rt)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCES_INTERDIT"));
	}

	@Test
	void utilisateur_multi_roles_obtient_l_union_des_permissions() throws Exception {
		Utilisateur adminRt = creerUtilisateur(
				"AdminRT", "adminrt@example.com", true, Role.ADMINISTRATEUR, Role.RESPONSABLE_TECHNIQUE);
		String jeton = BEARER_PREFIX + jeton(adminRt);

		// Administration (ADMIN) :
		mockMvc.perform(get("/api/v1/utilisateurs").header("Authorization", jeton))
				.andExpect(status().isOk());

		// Route RT (RESPONSABLE_TECHNIQUE) : barrière franchie → 404 au Bloc 3.
		mockMvc.perform(post("/api/v1/demandes")
						.header("Authorization", jeton)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"titre\":\"t\"}"))
				.andExpect(status().isNotFound());

		// Route AT (absente des rôles) : refus.
		mockMvc.perform(post("/api/v1/demandes/REF-1/resolution")
						.header("Authorization", jeton))
				.andExpect(status().isForbidden());
	}

	@Test
	void login_est_public_sans_jwt() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"inconnu@example.com","password":"MotDePasseDeTest"}
								"""))
				.andExpect(status().isUnauthorized());
	}
}
