package com.akrouty.gestiondemandes.security.presentation;

import com.akrouty.gestiondemandes.security.application.AuthenticationService;
import com.akrouty.gestiondemandes.security.application.AuthentificationResultat;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Point d'entrée public d'authentification : {@code POST /api/v1/auth/login}.
 *
 * <p>Seul endpoint accessible sans JWT. Aucun {@code /me}, aucun logout
 * backend, aucun refresh token en V1 (ADR-004 §13, ADR-005 §3).</p>
 */
@RestController
public class AuthController {

	private final AuthenticationService authenticationService;

	public AuthController(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

	@PostMapping("/api/v1/auth/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		AuthentificationResultat resultat = authenticationService.connecter(request.email(), request.password());
		return ResponseEntity.ok(new LoginResponse(
				resultat.accessToken(), resultat.tokenType(), resultat.expiresAt(), resultat.user()));
	}
}