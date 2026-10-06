package com.akrouty.gestiondemandes.security.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.akrouty.gestiondemandes.identity.domain.Role;
import com.akrouty.gestiondemandes.identity.domain.Utilisateur;
import com.akrouty.gestiondemandes.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Scénarios essentiels VALIDÉS de relecture Identity à chaque requête
 * (SECURITY-DESIGN-V1 §15) : les rôles et l'état actif faisant autorité sont
 * relus en base — le JWT émis n'a pas besoin d'être réémis.
 */
class RelectureIdentiteTest extends ApiTestSupport {

	@Test
	void role_retire_apres_login_est_perdu_des_la_requete_suivante_avec_le_meme_jwt() throws Exception {
		Utilisateur admin = creerUtilisateur("Admin", "admin@example.com", true, Role.ADMINISTRATEUR);
		Utilisateur rt = creerUtilisateur("RT", "rt@example.com", true, Role.RESPONSABLE_TECHNIQUE);
		String jetonAdmin = BEARER_PREFIX + jeton(admin);
		String jetonRt = BEARER_PREFIX + jeton(rt);

		// 1-4. Le rôle est autorisé : la barrière RBAC laisse passer la requête
		// (aucun contrôleur métier avant le Bloc 3 → 404, jamais 401/403).
		mockMvc.perform(get("/api/v1/demandes").header("Authorization", jetonRt))
				.andExpect(status().isNotFound());

		// 5. Retrait du rôle EN BASE via le cas d'utilisation d'administration.
		mockMvc.perform(put("/api/v1/utilisateurs/" + rt.getId() + "/roles-metier")
						.header("Authorization", jetonAdmin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"rolesMetier":[]}
								"""))
				.andExpect(status().isOk());

		// 6-8. EXACTEMENT le même JWT : les rôles actuels font autorité.
		mockMvc.perform(get("/api/v1/demandes").header("Authorization", jetonRt))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCES_INTERDIT"));
	}

	@Test
	void compte_desactive_apres_login_est_refuse_avec_le_meme_jwt() throws Exception {
		Utilisateur admin = creerUtilisateur("Admin", "admin@example.com", true, Role.ADMINISTRATEUR);
		Utilisateur cible = creerUtilisateur("Cible", "cible@example.com", true, Role.ADMINISTRATEUR);
		String jetonAdmin = BEARER_PREFIX + jeton(admin);
		String jetonCible = BEARER_PREFIX + jeton(cible);

		// Le compte est actif : requête autorisée.
		mockMvc.perform(get("/api/v1/utilisateurs").header("Authorization", jetonCible))
				.andExpect(status().isOk());

		// Désactivation via l'administration.
		mockMvc.perform(post("/api/v1/utilisateurs/" + cible.getId() + "/desactivation")
						.header("Authorization", jetonAdmin))
				.andExpect(status().isOk());

		// Même JWT toujours valide chronologiquement : 401 car actif relu en base.
		mockMvc.perform(get("/api/v1/utilisateurs").header("Authorization", jetonCible))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTIFICATION_REQUISE"));
	}
}