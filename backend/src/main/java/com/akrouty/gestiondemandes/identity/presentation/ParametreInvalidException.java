package com.akrouty.gestiondemandes.identity.presentation;

/**
 * Paramètre de requête invalide (pagination ou tri). Traduit en
 * {@code 400 Bad Request} sans révéler de détail interne.
 */
public class ParametreInvalidException extends RuntimeException {

	public ParametreInvalidException(String message) {
		super(message);
	}
}