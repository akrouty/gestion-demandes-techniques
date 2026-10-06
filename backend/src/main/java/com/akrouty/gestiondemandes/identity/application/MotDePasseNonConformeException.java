package com.akrouty.gestiondemandes.identity.application;

/**
 * Mot de passe refusé par la politique minimale (présence, non-blanc,
 * longueur minimale configurée). Traduit en {@code 400 Bad Request}.
 *
 * <p>Aucune règle de complexité supplémentaire n'est appliquée : aucune n'a
 * été validée dans les documents de référence. Le mot de passe n'est jamais
 * tronqué ni journalisé.</p>
 */
public class MotDePasseNonConformeException extends RuntimeException {

	public MotDePasseNonConformeException() {
		super("Mot de passe non conforme à la politique minimale");
	}
}