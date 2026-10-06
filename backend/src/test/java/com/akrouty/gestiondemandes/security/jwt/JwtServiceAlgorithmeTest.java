package com.akrouty.gestiondemandes.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.akrouty.gestiondemandes.security.config.JwtProperties;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Émission + validation réelles pour chacun des algorithmes HMAC acceptés
 * par {@code JWT_ALGORITHM} (HS256, HS384, HS512).
 *
 * <p>Le même {@link MacAlgorithm} serveur est transmis EXPLICITEMENT à
 * l'émetteur et au validateur ; un token signé avec un autre algorithme
 * HMAC est refusé, quel que soit l'algorithme configuré.</p>
 */
class JwtServiceAlgorithmeTest {

	/**
	 * Secret TEST de 72 octets : longueur suffisante pour les trois
	 * algorithmes HMAC (≥ 64 octets requis pour HS512). Donnée de test
	 * uniquement, jamais une configuration production.
	 */
	private static final String SECRET_TEST =
			"test-jwt-hmac-algo-secret-0123456789abcdefghijklmnopqrstuvwxyz0123456789";

	private static final String ISSUER_TEST = "jwt-algo-test-issuer";
	private static final Duration TTL_TEST = Duration.ofMinutes(5);

	private static JwtService servicePour(MacAlgorithm algorithme) {
		return new JwtService(new JwtProperties(
				SECRET_TEST, ISSUER_TEST, TTL_TEST, algorithme.getName()));
	}

	@ParameterizedTest
	@EnumSource(MacAlgorithm.class)
	void token_emis_porte_l_algorithme_configure_et_est_valide(MacAlgorithm algorithme) {
		JwtService service = servicePour(algorithme);

		JwtService.TokenEmis emis = service.emettre(42L);

		// Le token émis porte exactement l'algorithme configuré.
		String algorithmeDuToken = parser(emis.valeur()).getHeader().getAlgorithm().getName();
		assertThat(algorithmeDuToken).isEqualTo(algorithme.getName());

		// Ce même token est validé par le service (alg, signature, exp, iss).
		Jwt valide = service.valider(emis.valeur());
		assertThat(valide.getSubject()).isEqualTo("42");
		// getClaimAsString : Jwt.getIssuer() convertit en URL et notre issuer
		// de test (comme ceux de la V1) n'est pas une URL.
		assertThat(valide.getClaimAsString("iss")).isEqualTo(ISSUER_TEST);
		assertThat(valide.getExpiresAt()).isEqualTo(emis.expiresAt());
	}

	@ParameterizedTest
	@EnumSource(MacAlgorithm.class)
	void token_portant_un_autre_algorithme_hmac_est_refuse(MacAlgorithm algorithme) {
		JwtService service = servicePour(algorithme);

		String jetonAutreAlgorithme = fabriquer(autreAlgorithme(algorithme));

		assertThatThrownBy(() -> service.valider(jetonAutreAlgorithme))
				.isInstanceOf(JwtException.class);
	}

	/** Un autre algorithme HMAC que celui configuré (toujours HS256/HS384/HS512). */
	private static MacAlgorithm autreAlgorithme(MacAlgorithm algorithme) {
		return switch (algorithme) {
			case HS256 -> MacAlgorithm.HS512;
			case HS384 -> MacAlgorithm.HS256;
			case HS512 -> MacAlgorithm.HS384;
		};
	}

	private static JWSAlgorithm versJws(MacAlgorithm algorithme) {
		return switch (algorithme) {
			case HS256 -> JWSAlgorithm.HS256;
			case HS384 -> JWSAlgorithm.HS384;
			case HS512 -> JWSAlgorithm.HS512;
		};
	}

	private static SignedJWT parser(String jeton) {
		try {
			return SignedJWT.parse(jeton);
		} catch (java.text.ParseException e) {
			throw new IllegalStateException("Jeton de test illisible", e);
		}
	}

	/** Fabrique un token correct (issuer, dates valides) signé avec l'algorithme imposé. */
	private static String fabriquer(MacAlgorithm algorithme) {
		try {
			Instant iat = Instant.now().truncatedTo(ChronoUnit.SECONDS);
			JWTClaimsSet claims = new JWTClaimsSet.Builder()
					.subject("42")
					.issuer(ISSUER_TEST)
					.issueTime(Date.from(iat))
					.expirationTime(Date.from(iat.plus(TTL_TEST)))
					.build();
			SignedJWT jwt = new SignedJWT(new JWSHeader(versJws(algorithme)), claims);
			jwt.sign(new MACSigner(SECRET_TEST.getBytes(StandardCharsets.UTF_8)));
			return jwt.serialize();
		} catch (com.nimbusds.jose.JOSEException e) {
			throw new IllegalStateException("Fabrication de jeton de test impossible", e);
		}
	}
}