/**
 * Module de sécurité : authentification JWT stateless, rélecture Identity à
 * chaque requête protégée et RBAC (Bloc 2).
 *
 * <p>Dépend de la responsabilité publique {@code identity/application} —
 * jamais directement de {@code UtilisateurRepository} (ADR-002).</p>
 *
 * <p>Responsabilités : {@code config} (chaîne de sécurité, CORS, propriétés),
 * {@code jwt} (émission et validation HMAC du token), {@code application}
 * (cas d'utilisation de login et principal authentifié) et
 * {@code presentation} (point d'entrée login, filtre JWT, erreurs 401/403).</p>
 */
package com.akrouty.gestiondemandes.security;
