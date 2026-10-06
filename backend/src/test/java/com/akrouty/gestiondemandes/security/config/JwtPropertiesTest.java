package com.akrouty.gestiondemandes.security.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

/**
 * Tests de la validation de configuration JWT : une configuration critique
 * absente ou invalide provoque un échec explicite, sans jamais révéler la
 * valeur d'un secret et sans aucun repli.
 */
class JwtPropertiesTest {

	private static final String SECRET_VALIDE =
			"test-jwt-secret-de-longueur-suffisante-0123456789abcdef";

	private static JwtProperties configurationValide() {
		return new JwtProperties(SECRET_VALIDE, "issuer-de-test", Duration.ofMinutes(15), "HS256");
	}

	@Test
	void configuration_valide_est_acceptee() {
		JwtProperties proprietes = configurationValide();

		assertThatCode(proprietes::valider).doesNotThrowAnyException();
		assertThat(proprietes.algorithmeHmac()).isEqualTo(MacAlgorithm.HS256);
		assertThat(proprietes.cleHmac().getEncoded()).hasSize(SECRET_VALIDE.getBytes().length);
	}

	@Test
	void secret_absent_ou_vide_echoue_sans_secret_de_repli() {
		assertThatThrownBy(() -> new JwtProperties(null, "iss", Duration.ofMinutes(1), "HS256").valider())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("JWT_SECRET");

		assertThatThrownBy(() -> new JwtProperties("   ", "iss", Duration.ofMinutes(1), "HS256").valider())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("JWT_SECRET");
	}

	@Test
	void secret_trop_court_echoue_sans_jamais_reveler_sa_valeur() {
		String secretTropCourt = "topsecret";

		assertThatThrownBy(() -> new JwtProperties(secretTropCourt, "iss", Duration.ofMinutes(1), "HS256").valider())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("trop court")
				.satisfies(e -> assertThat(e.getMessage()).doesNotContain(secretTropCourt));
	}

	@Test
	void algorithme_absent_ou_non_hmac_echoue() {
		assertThatCode(() -> configurationValide().algorithmeHmac()).doesNotThrowAnyException();

		assertThatThrownBy(() -> new JwtProperties(SECRET_VALIDE, "iss", Duration.ofMinutes(1), null).valider())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("JWT_ALGORITHM");

		assertThatThrownBy(() -> new JwtProperties(SECRET_VALIDE, "iss", Duration.ofMinutes(1), "RS256").valider())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("JWT_ALGORITHM");
	}

	@Test
	void issuer_absent_echoue() {
		assertThatThrownBy(() -> new JwtProperties(SECRET_VALIDE, " ", Duration.ofMinutes(1), "HS256").valider())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("JWT_ISSUER");
	}

	@Test
	void ttl_absent_ou_non_positif_echoue() {
		assertThatThrownBy(() -> new JwtProperties(SECRET_VALIDE, "iss", null, "HS256").valider())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("JWT_TTL");

		assertThatThrownBy(() -> new JwtProperties(SECRET_VALIDE, "iss", Duration.ZERO, "HS256").valider())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("JWT_TTL");

		assertThatThrownBy(() -> new JwtProperties(SECRET_VALIDE, "iss", Duration.ofMinutes(-5), "HS256").valider())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("JWT_TTL");
	}
}