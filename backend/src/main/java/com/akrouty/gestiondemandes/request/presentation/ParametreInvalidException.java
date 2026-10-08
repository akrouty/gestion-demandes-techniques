package com.akrouty.gestiondemandes.request.presentation;

/** Paramètre de requête invalide (pagination ou tri). Traduit en {@code 400}. */
public class ParametreInvalidException extends RuntimeException {

	public ParametreInvalidException(String message) {
		super(message);
	}
}