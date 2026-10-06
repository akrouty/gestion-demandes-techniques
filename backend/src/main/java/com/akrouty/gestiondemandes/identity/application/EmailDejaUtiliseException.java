package com.akrouty.gestiondemandes.identity.application;

/**
 * Email utilisateur déjà utilisé. Traduit en {@code 409 Conflict}.
 * Aucun détail de contrainte de base n'est exposé au client.
 */
public class EmailDejaUtiliseException extends RuntimeException {

	public EmailDejaUtiliseException() {
		super("Email déjà utilisé");
	}
}