package com.akrouty.gestiondemandes.request.domain;

/**
 * Codes centralisés des événements d'historique ({@code typeEvenement} reste un
 * {@code String} conformément au modèle actuel — aucun nouvel enum métier).
 *
 * <p>Ces codes sont INTERNES à l'historisation : ils ne constituent pas un
 * contrat REST et ne sont pas exposés comme tels.</p>
 */
public final class EvenementsDemande {

	public static final String CREATION = "CREATION";
	public static final String CATEGORIE_MODIFIEE = "CATEGORIE_MODIFIEE";
	public static final String PRIORITE_MODIFIEE = "PRIORITE_MODIFIEE";
	public static final String AFFECTATION = "AFFECTATION";
	public static final String REAFFECTATION = "REAFFECTATION";
	public static final String TRAITEMENT_DEMARRE = "TRAITEMENT_DEMARRE";
	public static final String DESCRIPTION_TRAITEMENT_MODIFIEE = "DESCRIPTION_TRAITEMENT_MODIFIEE";
	public static final String SOLUTION_MODIFIEE = "SOLUTION_MODIFIEE";
	public static final String RESOLUTION = "RESOLUTION";
	public static final String REFUS_RESOLUTION = "REFUS_RESOLUTION";
	public static final String CLOTURE = "CLOTURE";
	public static final String ANNULATION = "ANNULATION";

	private EvenementsDemande() {
	}
}
