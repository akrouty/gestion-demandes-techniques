package com.akrouty.gestiondemandes.security.jwt;

import com.akrouty.gestiondemandes.security.config.JwtProperties;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.crypto.SecretKey;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Émission et validation du JWT d'accès (HMAC, monolithe V1 — ADR-005).
 *
 * <p>Claims émis : {@code sub} (identifiant technique de l'utilisateur),
 * {@code iat}, {@code exp = iat + TTL configuré}, {@code iss}. Aucun claim de
 * rôles, aucun {@code aud}, aucun credential : les rôles sont toujours relus
 * depuis Identity à chaque requête protégée.</p>
 *
 * <p>Validation : signature HMAC, algorithme ÉPINGLÉ à la configuration
 * serveur (le champ {@code alg} du token n'est jamais suivi), expiration
 * (sans tolérance) et issuer. Tout échec est signalé par {@link JwtException}
 * puis traduit en {@code 401} générique à la frontière, sans révéler de
 * détail cryptographique.</p>
 */
public class JwtService {

	private final JwtEncoder encodeur;
	private final JwtDecoder decodeur;
	private final JwtProperties proprietes;
	private final String nomAlgorithme;

	public JwtService(JwtProperties proprietes) {
		proprietes.valider();
		this.proprietes = proprietes;
		MacAlgorithm algorithme = proprietes.algorithmeHmac();
		this.nomAlgorithme = algorithme.getName();
		SecretKey cle = proprietes.cleHmac();

		this.encodeur = NimbusJwtEncoder.withSecretKey(cle).build();

		NimbusJwtDecoder decodeurHmac = NimbusJwtDecoder.withSecretKey(cle).build();
		OAuth2TokenValidator<Jwt> validateurTemps = new JwtTimestampValidator(Duration.ZERO);
		OAuth2TokenValidator<Jwt> validateurIssuer = new JwtIssuerValidator(proprietes.issuer());
		decodeurHmac.setJwtValidator(jwt -> {
			OAuth2TokenValidatorResult resultat = validateurTemps.validate(jwt);
			return resultat.hasErrors() ? resultat : validateurIssuer.validate(jwt);
		});
		this.decodeur = decodeurHmac;
	}

	/** Émet un token pour l'utilisateur : {@code exp} exactement égal à {@code iat + TTL}. */
	public TokenEmis emettre(Long utilisateurId) {
		Instant iat = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		Instant exp = iat.plus(proprietes.ttl()).truncatedTo(ChronoUnit.SECONDS);

		JwsHeader header = JwsHeader.with(proprietes.algorithmeHmac()).type("JWT").build();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(String.valueOf(utilisateurId))
				.issuer(proprietes.issuer())
				.issuedAt(iat)
				.expiresAt(exp)
				.build();

		Jwt jwt = encodeur.encode(JwtEncoderParameters.from(header, claims));
		return new TokenEmis(jwt.getTokenValue(), exp);
	}

	/**
	 * Valide un token reçu : algorithme épinglé à la configuration puis
	 * signature, expiration et issuer. Lance {@link JwtException} sinon.
	 */
	public Jwt valider(String jeton) {
		verifierAlgorithmeConfigure(jeton);
		return decodeur.decode(jeton);
	}

	public String nomAlgorithme() {
		return nomAlgorithme;
	}

	/** L'algorithme du token doit correspondre exactement à celui du serveur. */
	private void verifierAlgorithmeConfigure(String jeton) {
		try {
			SignedJWT signe = SignedJWT.parse(jeton);
			JWSAlgorithm algorithmeToken = signe.getHeader().getAlgorithm();
			if (algorithmeToken == null || !nomAlgorithme.equals(algorithmeToken.getName())) {
				throw new JwtException("Algorithme du token non conforme à la configuration serveur");
			}
		} catch (ParseException e) {
			throw new JwtException("Token malformé");
		}
	}

	/** Token émis : valeur brute et expiration ISO-8601, strictement cohérente avec {@code exp}. */
	public record TokenEmis(String valeur, Instant expiresAt) {
	}
}