package com.akrouty.gestiondemandes.security.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.akrouty.gestiondemandes.identity.domain.Role;
import com.akrouty.gestiondemandes.identity.domain.Utilisateur;
import com.akrouty.gestiondemandes.support.ApiTestSupport;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests négatifs du JWT : token absent, invalide, altéré, expiré, mauvais
 * issuer et mauvais algorithme → tous {@code 401} générique, sans détail
 * cryptographique. Les tokens sont fabriqués manuellement avec Nimbus
 * (dépendance JOSE déjà présente via Spring Security).
 */
class JwtSecuriteTest extends ApiTestSupport {

	private String jetonAdmin;
	private Long idAdmin;

	@BeforeEach
	void preparerAdministrateur() {
		Utilisateur admin = creerUtilisateur("Admin", "admin@example.com", true, Role.ADMINISTRATEUR);
		idAdmin = admin.getId();
		jetonAdmin = jeton(admin);
	}

	@Test
	void sans_jwt_401_avec_code_stable() throws Exception {
		mockMvc.perform(get("/api/v1/utilisateurs"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTIFICATION_REQUISE"));
	}

	@Test
	void jwt_invalide_ou_altere_401() throws Exception {
		// Token syntaxiquement invalide.
		mockMvc.perform(get("/api/v1/utilisateurs")
						.header("Authorization", BEARER_PREFIX + "abc.def.ghi"))
				.andExpect(status().isUnauthorized());

		// En-tête Authorization non porteur.
		mockMvc.perform(get("/api/v1/utilisateurs")
						.header("Authorization", "Basic dXNlcjpwYXNz"))
				.andExpect(status().isUnauthorized());

		// Jeton signé puis modifié dans son payload (signature non conforme).
		String altere = altererPayload(jetonAdmin);
		mockMvc.perform(get("/api/v1/utilisateurs")
						.header("Authorization", BEARER_PREFIX + altere))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTIFICATION_REQUISE"));
	}

	@Test
	void jwt_expire_401() throws Exception {
		Instant passe = Instant.now().minus(10, ChronoUnit.MINUTES);
		String expire = fabriquer(JWSAlgorithm.HS256, ISSUER_DE_TEST, SECRET_JWT_DE_TEST,
				passe.minus(5, ChronoUnit.MINUTES), passe, String.valueOf(idAdmin));

		mockMvc.perform(get("/api/v1/utilisateurs")
						.header("Authorization", BEARER_PREFIX + expire))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTIFICATION_REQUISE"));
	}

	@Test
	void jwt_avec_issuer_incorrect_401() throws Exception {
		Instant iat = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		String autreIssuer = fabriquer(JWSAlgorithm.HS256, "autre-issuer", SECRET_JWT_DE_TEST,
				iat, iat.plus(5, ChronoUnit.MINUTES), String.valueOf(idAdmin));

		mockMvc.perform(get("/api/v1/utilisateurs")
						.header("Authorization", BEARER_PREFIX + autreIssuer))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void jwt_avec_algorithme_different_de_la_configuration_401() throws Exception {
		// Algorithme HMAC différent de HS256 configuré côté serveur : refusé,
		// le champ alg du token n'est jamais suivi.
		Instant iat = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		String hs512 = fabriquer(JWSAlgorithm.HS512, ISSUER_DE_TEST, SECRET_JWT_DE_TEST,
				iat, iat.plus(5, ChronoUnit.MINUTES), String.valueOf(idAdmin));

		mockMvc.perform(get("/api/v1/utilisateurs")
						.header("Authorization", BEARER_PREFIX + hs512))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void jwt_signe_avec_un_autre_secret_401() throws Exception {
		Instant iat = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		String autreSecret = fabriquer(JWSAlgorithm.HS256, ISSUER_DE_TEST,
				"autre-cle-hmac-complementaire-pour-le-test-0123456789",
				iat, iat.plus(5, ChronoUnit.MINUTES), String.valueOf(idAdmin));

		mockMvc.perform(get("/api/v1/utilisateurs")
						.header("Authorization", BEARER_PREFIX + autreSecret))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void aucune_reponse_401_ne_revele_un_detail_cryptographique() throws Exception {
		String corps = corps(mockMvc.perform(get("/api/v1/utilisateurs")
						.header("Authorization", BEARER_PREFIX + "token.invalide"))
				.andExpect(status().isUnauthorized())
				.andReturn());

		assertThat(corps).doesNotContain("signature");
		assertThat(corps).doesNotContain("secret");
		assertThat(corps).doesNotContain("expir");
		assertThat(corps).doesNotContain("Exception");
	}

	// ---------------------------------------------------------------- utilitaires de test

	/** Fabrique un JWT signé HMAC avec des paramètres librement choisis (test). */
	private String fabriquer(
			JWSAlgorithm algorithme,
			String issuer,
			String secret,
			Instant iat,
			Instant exp,
			String sub) {
		try {
			JWTClaimsSet claims = new JWTClaimsSet.Builder()
					.subject(sub)
					.issuer(issuer)
					.issueTime(Date.from(iat))
					.expirationTime(Date.from(exp))
					.build();
			SignedJWT jwt = new SignedJWT(new JWSHeader(algorithme), claims);
			jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
			return jwt.serialize();
		} catch (com.nimbusds.jose.JOSEException e) {
			throw new IllegalStateException("Fabrication de jeton de test impossible", e);
		}
	}

	/** Modifie un caractère du payload : la signature ne correspond plus. */
	private String altererPayload(String jeton) {
		String[] parties = jeton.split("\\.");
		String payload = parties[1];
		char original = payload.charAt(6);
		parties[1] = payload.substring(0, 6) + (original == 'q' ? 'r' : 'q') + payload.substring(7);
		return String.join(".", parties);
	}
}
