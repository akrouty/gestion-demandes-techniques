package com.akrouty.gestiondemandes.request.application;

/**
 * Contrôle métier contextuel refusé (ex. l'Agent connecté n'est pas l'Agent
 * affecté à la demande). Traduit en {@code 403 Forbidden} par presentation.
 */
public class AccesDemandeInterditException extends RuntimeException {

	public AccesDemandeInterditException(String message) {
		super(message);
	}
}
