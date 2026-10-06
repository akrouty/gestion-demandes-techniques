package com.akrouty.gestiondemandes.identity.application;

import com.akrouty.gestiondemandes.identity.domain.Role;
import java.util.Set;

/**
 * Commande de création d'un utilisateur (valeur d'entrée du cas d'utilisation).
 *
 * @param nom          nom obligatoire non blanc
 * @param email        email obligatoire ; normalisé par l'application
 * @param actif        état fonctionnel initial explicite
 * @param rolesMetier  rôles métier autorisés à la création (RT et/ou AT)
 * @param password     mot de passe en clair, donnée d'entrée uniquement
 */
public record CreationUtilisateurCommand(
		String nom,
		String email,
		boolean actif,
		Set<Role> rolesMetier,
		String password) {
}