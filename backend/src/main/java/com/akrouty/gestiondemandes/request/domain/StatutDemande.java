package com.akrouty.gestiondemandes.request.domain;

/**
 * Statuts du cycle de vie d'une demande, persistés comme codes textuels
 * stables. Les invariants de transition sont appliqués dans les blocs
 * suivants, pas dans ce bloc.
 */
public enum StatutDemande {

	NOUVELLE,
	ASSIGNEE,
	EN_COURS,
	RESOLUE,
	CLOTUREE,
	ANNULEE
}
