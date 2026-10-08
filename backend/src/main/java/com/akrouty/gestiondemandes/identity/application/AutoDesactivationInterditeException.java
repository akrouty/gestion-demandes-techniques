package com.akrouty.gestiondemandes.identity.application;

/** Refus contextuel de la désactivation du compte authentifié. */
public class AutoDesactivationInterditeException extends RuntimeException {
	public AutoDesactivationInterditeException() {
		super("Vous ne pouvez pas désactiver votre propre compte.");
	}
}
