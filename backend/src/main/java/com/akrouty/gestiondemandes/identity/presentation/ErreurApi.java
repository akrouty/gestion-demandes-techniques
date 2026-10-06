package com.akrouty.gestiondemandes.identity.presentation;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Format d'erreur commun de l'API (API-CONTRACT-V1) :
 * {@code code} stable, {@code message}, {@code fieldErrors} optionnel.
 * Aucune stack trace, exception Java, SQL ou détail interne n'est exposé.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErreurApi(String code, String message, List<ChampErreur> fieldErrors) {

	public static ErreurApi simple(String code, String message) {
		return new ErreurApi(code, message, null);
	}

	public record ChampErreur(String field, String code, String message) {
	}
}