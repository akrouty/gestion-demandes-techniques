package com.akrouty.gestiondemandes.security.application;

import java.time.Instant;

/** Résultat applicatif de connexion, indépendant des DTO HTTP. */
public record AuthentificationResultat(
        String accessToken, String tokenType, Instant expiresAt, UtilisateurConnecte user) {
}
