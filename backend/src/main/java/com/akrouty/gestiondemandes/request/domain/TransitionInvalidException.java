package com.akrouty.gestiondemandes.request.domain;

/**
 * Transition ou état incompatible avec l'action métier demandée (cycle de vie
 * sans State Pattern, ADR-002).
 *
 * <p>Exception de domaine : aucun code HTTP n'y figure ; la traduction en
 * {@code 409 Conflict} appartient à la couche presentation.</p>
 */
public class TransitionInvalidException extends RuntimeException {

	public TransitionInvalidException(String message) {
		super(message);
	}
}
