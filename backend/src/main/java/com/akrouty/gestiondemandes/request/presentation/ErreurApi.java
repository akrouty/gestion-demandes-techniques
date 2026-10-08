package com.akrouty.gestiondemandes.request.presentation;

import java.util.List;

/**
 * Format d'erreur commun de l'API (API-CONTRACT-V1 §8) pour le module
 * {@code request} : {@code code} stable, {@code message}, {@code fieldErrors}
 * optionnel. Aucune stack trace, exception Java, SQL ou détail interne.
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ErreurApi(String code, String message, List<ChampErreur> fieldErrors) {

	public static ErreurApi simple(String code, String message) {
		return new ErreurApi(code, message, null);
	}

	public record ChampErreur(String field, String code, String message) {
	}
}
