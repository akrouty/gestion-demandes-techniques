package com.akrouty.gestiondemandes.request.domain;

/**
 * La demande est dans un état terminal ({@code CLOTUREE} ou {@code ANNULEE})
 * et n'est plus modifiable fonctionnellement (RM43).
 *
 * <p>Exception de domaine : elle ne connaît aucun code HTTP ; la traduction en
 * {@code 409 Conflict} appartient à la couche presentation.</p>
 */
public class DemandeTermineeException extends RuntimeException {

	public DemandeTermineeException(String reference) {
		super("Demande terminée : " + reference);
	}
}
