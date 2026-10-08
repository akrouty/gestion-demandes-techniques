package com.akrouty.gestiondemandes.request.domain;

/**
 * Une demande ne peut devenir {@code RESOLUE} sans solution non blanche (RM33).
 *
 * <p>Exception de domaine : aucun code HTTP n'y figure ; la traduction en
 * {@code 409 Conflict} appartient à la couche presentation.</p>
 */
public class SolutionRequiseException extends RuntimeException {

	public SolutionRequiseException() {
		super("Une solution est requise avant la résolution.");
	}
}
