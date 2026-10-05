package com.akrouty.gestiondemandes.request.domain;

/**
 * Catégories V1 validées. Le code technique persisté est distinct du libellé
 * français affiché ; il est persisté comme texte stable (ADR-003).
 */
public enum Categorie {

	ETUDE_DANGERS,
	ANALYSE_RISQUES_INDUSTRIELS,
	PROTECTION_INCENDIE,
	NOTE_CALCUL,
	DOSSIER_TECHNIQUE,
	ASSISTANCE_REGLEMENTAIRE,
	AUTRE
}
