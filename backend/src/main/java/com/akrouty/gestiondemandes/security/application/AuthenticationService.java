package com.akrouty.gestiondemandes.security.application;

import com.akrouty.gestiondemandes.identity.application.IdentiteConnexion;
import com.akrouty.gestiondemandes.identity.application.IdentiteService;
import com.akrouty.gestiondemandes.security.jwt.JwtService;
import com.akrouty.gestiondemandes.security.presentation.LoginRequest;
import com.akrouty.gestiondemandes.security.presentation.LoginResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Cas d'utilisation {@code POST /api/v1/auth/login} (ADR-005,
 * SECURITY-DESIGN-V1 §2).
 *
 * <p>Flux : normalisation et recherche de l'identité via Identity, puis —
 * pour un compte absent, inactif ou un mot de passe incorrect — exactement le
 * même échec générique {@code 401}. Jamais d'indice sur l'existence ou l'état
 * d'un compte. Le mot de passe en clair n'est ni journalisé ni conservé.</p>
 */
@Service
public class AuthenticationService {

	/** Type du token retourné (RFC 6750), strictement {@code Bearer}. */
	public static final String TYPE_BEARER = "Bearer";

	private final IdentiteService identiteService;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthenticationService(
			IdentiteService identiteService,
			PasswordEncoder passwordEncoder,
			JwtService jwtService) {
		this.identiteService = identiteService;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	public LoginResponse connecter(LoginRequest demande) {
		IdentiteConnexion identite = identiteService
				.rechercherPourConnexion(demande.email())
				.orElse(null);

		// Échec générique unique : inconnu, inactif et mauvais mot de passe
		// sont indistinguables côté client (401).
		if (identite == null
				|| !identite.actif()
				|| demande.password() == null
				|| !passwordEncoder.matches(demande.password(), identite.passwordHash())) {
			throw new AuthentificationEchoueeException();
		}

		JwtService.TokenEmis jeton = jwtService.emettre(identite.id());
		return new LoginResponse(
				jeton.valeur(),
				TYPE_BEARER,
				jeton.expiresAt(),
				new UtilisateurConnecte(identite.id(), identite.nom(), identite.email(), identite.roles()));
	}
}