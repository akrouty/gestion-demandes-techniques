package com.akrouty.gestiondemandes.security.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.akrouty.gestiondemandes.identity.domain.Role;
import com.akrouty.gestiondemandes.identity.domain.Utilisateur;
import com.akrouty.gestiondemandes.support.ApiTestSupport;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Tests de {@code POST /api/v1/auth/login} : réponse complète, échec
 * générique unique (401 identique pour inconnu / inactif / mauvais mot de
 * passe), normalisation email et structure des claims JWT.
 */
class LoginApiTest extends ApiTestSupport {

	/** TTL TEST de {@code src/test/resources/application.properties} : PT5M. */
	private static final long TTL_TEST_SECONDES = 300;

	private MvcResult login(String email, String password) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
				.andReturn();
	}

	@Test
	void login_valide_retourne_token_bearer_expiration_et_snapshot() throws Exception {
		Utilisateur admin = creerUtilisateur(
				"Admin Principal", "admin@example.com", true, Role.ADMINISTRATEUR, Role.RESPONSABLE_TECHNIQUE);

		MvcResult resultat = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"admin@example.com","password":"MotDePasseDeTest"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.user.id").value(admin.getId().intValue()))
				.andExpect(jsonPath("$.user.nom").value("Admin Principal"))
				.andExpect(jsonPath("$.user.email").value("admin@example.com"))
				.andExpect(jsonPath("$.user.roles.length()").value(2))
				.andReturn();

		String corps = corps(resultat);
		assertThat(corps).doesNotContain("password");
		assertThat(corps).doesNotContain("passwordHash");

		String jeton = JsonPath.read(corps, "$.accessToken");
		assertThat(jeton.split("\\.")).hasSize(3);

		Map<String, Object> claims = claimsDuJeton(jeton);
		long exp = ((Number) claims.get("exp")).longValue();
		long iat = ((Number) claims.get("iat")).longValue();

		// expiresAt ISO-8601 strictement égal au claim exp du JWT.
		Instant expiresAt = Instant.parse(JsonPath.read(corps, "$.expiresAt"));
		assertThat(expiresAt.getEpochSecond()).isEqualTo(exp);
		assertThat(exp - iat).isEqualTo(TTL_TEST_SECONDES);

		assertThat(claims.get("sub")).isEqualTo(String.valueOf(admin.getId()));
		assertThat(claims.get("iss")).isEqualTo(ISSUER_DE_TEST);
		// Les rôles et credentials ne sont jamais des claims d'autorité.
		assertThat(claims).doesNotContainKey("roles");
		assertThat(claims).doesNotContainKey("password");
		assertThat(claims).doesNotContainKey("passwordHash");
		assertThat(claims).doesNotContainKey("aud");
	}

	@Test
	void login_normalise_l_email_entree() throws Exception {
		creerUtilisateur("Bob", "bob@example.com", true, Role.AGENT_TECHNIQUE);

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"  BOB@Example.COM  ","password":"MotDePasseDeTest"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.user.email").value("bob@example.com"));
	}

	@Test
	void echec_genérique_identique_pour_inconnu_mauvais_mot_de_passe_et_inactif() throws Exception {
		Utilisateur actif = creerUtilisateur("Actif", "actif@example.com", true, Role.AGENT_TECHNIQUE);
		Utilisateur inactif = creerUtilisateur("Inactif", "inactif@example.com", false, Role.AGENT_TECHNIQUE);

		MvcResult inconnu = login("inconnu@example.com", "MotDePasseDeTest");
		MvcResult mauvaisMotDePasse = login(actif.getEmail(), "MotDePasseFaux");
		MvcResult compteInactif = login(inactif.getEmail(), MOT_DE_PASSE_DE_TEST);

		assertThat(inconnu.getResponse().getStatus()).isEqualTo(401);
		assertThat(mauvaisMotDePasse.getResponse().getStatus()).isEqualTo(401);
		assertThat(compteInactif.getResponse().getStatus()).isEqualTo(401);

		String corpsInconnu = corps(inconnu);
		assertThat(corpsInconnu).isEqualTo(corps(mauvaisMotDePasse));
		assertThat(corpsInconnu).isEqualTo(corps(compteInactif));
		assertThat(corpsInconnu).contains("AUTHENTIFICATION_ECHOUEE");
		// Aucune fuite sur l'existence ou l'état d'un compte.
		assertThat(corpsInconnu).doesNotContain("actif");
		assertThat(corpsInconnu).doesNotContain("existant");
	}

	@Test
	void login_sans_password_ou_sans_email_retourne_400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"bob@example.com"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION"));
	}

	/** Décode les claims du payload JWT (test uniquement, sans librairie tierce). */
	private Map<String, Object> claimsDuJeton(String jeton) {
		String payload = jeton.split("\\.")[1];
		String json = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
		return JsonPath.parse(json).read("$");
	}
}
