package com.akrouty.gestiondemandes.security.presentation;

import com.akrouty.gestiondemandes.security.application.UtilisateurConnecte;
import java.time.Instant;

/**
 * Réponse de login (API-CONTRACT-V1) : {@code accessToken}, {@code tokenType}
 * ({@code Bearer}), {@code expiresAt} (ISO-8601, exactement égal au claim
 * {@code exp} du JWT) et le snapshot UX {@code user}.
 *
 * <p>Aucun mot de passe, aucun hash, aucun claim de sécurité supplémentaire.
 * Ce snapshot n'est jamais une source d'autorisation.</p>
 */
public record LoginResponse(String accessToken, String tokenType, Instant expiresAt, UtilisateurConnecte user) {
}