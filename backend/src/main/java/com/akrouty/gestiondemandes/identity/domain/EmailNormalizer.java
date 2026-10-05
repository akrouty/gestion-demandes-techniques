package com.akrouty.gestiondemandes.identity.domain;

import java.util.Locale;

/**
 * Normalisation minimale et déterministe de l'email avant persistance (ADR-003).
 *
 * <p>Procédure : {@code trim} puis casse minuscule avec {@link Locale#ROOT}.
 * La normalisation exacte reste réutilisable par la couche application
 * (ex. : authentification) dans les blocs suivants.</p>
 */
public final class EmailNormalizer {

	private EmailNormalizer() {
	}

	public static String normalize(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
