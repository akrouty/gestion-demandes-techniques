package com.akrouty.gestiondemandes.security.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Origines CORS autorisées, externalisées via {@code CORS_ALLOWED_ORIGINS}
 * (liste séparée par des virgules).
 *
 * <p>Aucune origine par défaut, aucun wildcard : une liste vide n'autorise
 * aucune origine — elle n'ouvre jamais l'API arbitrairement.</p>
 *
 * @param allowedOrigins origines frontend explicitement autorisées
 */
@ConfigurationProperties(prefix = "app.security.cors")
public record CorsProperties(List<String> allowedOrigins) {

	/** Retourne la liste des origines, jamais {@code null}. */
	public List<String> originesNonNull() {
		return allowedOrigins == null ? List.of() : allowedOrigins;
	}
}