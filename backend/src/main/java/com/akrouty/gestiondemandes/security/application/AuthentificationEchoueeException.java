package com.akrouty.gestiondemandes.security.application;

/**
 * Échec d'authentification générique : compte inconnu, compte inactif ou
 * mot de passe incorrect produisent exactement la même erreur ({@code 401}).
 * La cause réelle n'est jamais révélée ni journalisée.
 */
public class AuthentificationEchoueeException extends RuntimeException {

	public AuthentificationEchoueeException() {
		super("Authentification refusée");
	}
}