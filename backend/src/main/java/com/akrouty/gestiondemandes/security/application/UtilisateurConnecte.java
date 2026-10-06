package com.akrouty.gestiondemandes.security.application;

import com.akrouty.gestiondemandes.identity.domain.Role;
import java.util.Set;

/**
 * Snapshot minimal de l'utilisateur au moment du login ({@code user} de
 * {@code LoginResponse}), destiné uniquement à l'UX Angular.
 *
 * <p>Jamais utilisé comme source d'autorisation : les rôles et l'état actif
 * faisant autorité sont relus depuis Identity à chaque requête protégée.</p>
 */
public record UtilisateurConnecte(Long id, String nom, String email, Set<Role> roles) {
}