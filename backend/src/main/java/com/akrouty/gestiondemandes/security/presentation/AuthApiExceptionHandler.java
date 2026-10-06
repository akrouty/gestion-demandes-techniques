package com.akrouty.gestiondemandes.security.presentation;

import com.akrouty.gestiondemandes.security.application.AuthentificationEchoueeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Erreurs du login : échec générique {@code 401} et validation de forme {@code 400}. */
@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthApiExceptionHandler {

	@ExceptionHandler(AuthentificationEchoueeException.class)
	public ResponseEntity<ErreurSecurite> echec(AuthentificationEchoueeException e) {
		// Identique pour compte inconnu, compte inactif et mot de passe incorrect.
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErreurSecurite.AUTHENTIFICATION_ECHOUEE);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErreurSecurite> validation(MethodArgumentNotValidException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ErreurSecurite("VALIDATION", "Requête invalide."));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErreurSecurite> corpsIllisible(HttpMessageNotReadableException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ErreurSecurite("REQUETE_INVALIDE", "Corps de requête invalide."));
	}
}