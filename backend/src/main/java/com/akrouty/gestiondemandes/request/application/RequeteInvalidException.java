package com.akrouty.gestiondemandes.request.application;

/** Requête invalide (forme ou règle de remplissage). Traduite en {@code 400 Bad Request}. */
public class RequeteInvalidException extends RuntimeException {

	public RequeteInvalidException(String message) {
		super(message);
	}
}
