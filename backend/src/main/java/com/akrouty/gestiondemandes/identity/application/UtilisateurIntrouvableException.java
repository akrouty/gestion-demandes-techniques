package com.akrouty.gestiondemandes.identity.application;

/** Utilisateur ciblé introuvable. Traduit en {@code 404 Not Found}. */
public class UtilisateurIntrouvableException extends RuntimeException {

	public UtilisateurIntrouvableException(Long id) {
		super("Utilisateur introuvable : " + id);
	}
}