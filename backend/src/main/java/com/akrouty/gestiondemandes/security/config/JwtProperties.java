package com.akrouty.gestiondemandes.security.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

/**
 * Paramètres JWT, entièrement externalisés (ADR-005) :
 * {@code JWT_SECRET}, {@code JWT_ISSUER}, {@code JWT_TTL}, {@code JWT_ALGORITHM}.
 *
 * <p>Aucun secret, issuer, durée ni algorithme de repli n'existe ici : une
 * configuration absente ou invalide provoque un échec explicite de
 * démarrage, sans jamais révéler la valeur d'un secret.</p>
 *
 * <p>L'algorithme est imposé par la configuration serveur ; le champ
 * {@code alg} reçu dans un token n'est jamais une source de confiance.</p>
 *
 * @param secret   secret HMAC (jamais versionné, jamais journalisé)
 * @param issuer   valeur du claim {@code iss}
 * @param ttl      durée de vie du token ({@code exp = iat + ttl})
 * @param algorithm algorithme HMAC attendu : HS256, HS384 ou HS512
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(String secret, String issuer, Duration ttl, String algorithm) {

	private static final int LONGUEUR_MINIMALE_HS256 = 32;
	private static final int LONGUEUR_MINIMALE_HS384 = 48;
	private static final int LONGUEUR_MINIMALE_HS512 = 64;

	/** Valide la configuration ; lance {@link IllegalStateException} si elle est absente ou invalide. */
	public void valider() {
		MacAlgorithm algorithme = algorithmeHmac();
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException(
					"JWT_SECRET absent ou vide : configuration de sécurité obligatoire, aucun secret de repli n'est accepté");
		}
		int minimal = longueurMinimaleSecret(algorithme);
		if (secret.getBytes(StandardCharsets.UTF_8).length < minimal) {
			throw new IllegalStateException(
					"JWT_SECRET trop court pour l'algorithme configuré : au moins " + minimal
							+ " octets sont requis (valeur du secret non révélée)");
		}
		if (issuer == null || issuer.isBlank()) {
			throw new IllegalStateException("JWT_ISSUER absent ou vide : configuration obligatoire");
		}
		if (ttl == null || ttl.isZero() || ttl.isNegative()) {
			throw new IllegalStateException("JWT_TTL invalide : durée obligatoire strictement positive");
		}
	}

	/** Algorithme HMAC exigé par la configuration ; échec si absent ou non supporté. */
	public MacAlgorithm algorithmeHmac() {
		if (algorithm == null || algorithm.isBlank()) {
			throw new IllegalStateException(
					"JWT_ALGORITHM absent : algorithme HMAC obligatoire (HS256, HS384 ou HS512)");
		}
		MacAlgorithm algorithme = MacAlgorithm.from(algorithm.trim().toUpperCase(Locale.ROOT));
		if (algorithme == null) {
			throw new IllegalStateException(
					"JWT_ALGORITHM non supporté : seul un algorithme HMAC de la V1 est accepté (HS256, HS384 ou HS512)");
		}
		return algorithme;
	}

	/** Clé HMAC dérivée du secret configuré. À n'utiliser qu'après {@link #valider()}. */
	public SecretKeySpec cleHmac() {
		if (secret == null) {
			throw new IllegalStateException("JWT_SECRET absent : configuration obligatoire");
		}
		String nomJws = algorithmeHmac().getName();
		return new SecretKeySpec(
				secret.getBytes(StandardCharsets.UTF_8),
				"HmacSHA" + nomJws.substring(2));
	}

	private static int longueurMinimaleSecret(MacAlgorithm algorithme) {
		return switch (algorithme) {
			case HS256 -> LONGUEUR_MINIMALE_HS256;
			case HS384 -> LONGUEUR_MINIMALE_HS384;
			case HS512 -> LONGUEUR_MINIMALE_HS512;
		};
	}
}