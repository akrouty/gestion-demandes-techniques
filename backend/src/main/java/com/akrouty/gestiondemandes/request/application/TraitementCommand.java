package com.akrouty.gestiondemandes.request.application;

/**
 * Commande de mise à jour du traitement. Les drapeaux {@code ...Fournie}
 * distinguent STRICTEMENT un champ absent (inchangé) d'un champ présent
 * (doit être non null et non blanc — validé à la frontière HTTP).
 */
public record TraitementCommand(
		String descriptionTraitement,
		boolean descriptionFournie,
		String solution,
		boolean solutionFournie) {
}
