package com.akrouty.gestiondemandes.request.application;

/** Demande ciblée introuvable. Traduite en {@code 404 Not Found} par presentation. */
public class DemandeIntrouvableException extends RuntimeException {

	public DemandeIntrouvableException(String reference) {
		super("Demande introuvable : " + reference);
	}
}
