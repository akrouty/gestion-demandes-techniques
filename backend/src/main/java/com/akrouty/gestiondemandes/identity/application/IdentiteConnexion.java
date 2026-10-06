package com.akrouty.gestiondemandes.identity.application;

import com.akrouty.gestiondemandes.identity.domain.Role;
import java.util.Set;

/**
 * Identité retournée au module {@code security} pour l'opération de connexion.
 *
 * <p>{@code passwordHash} est présent exclusivement pour la vérification
 * {@code PasswordEncoder.matches} du login : il n'est ni exposé en REST, ni
 * journalisé, ni utilisé comme autorité. {@code actif} et {@code roles} sont
 * lus en base au moment de la recherche ; la rélecture à chaque requête
 * protégée utilise {@link UtilisateurConsultation}.</p>
 */
public record IdentiteConnexion(
		Long id,
		String nom,
		String email,
		boolean actif,
		Set<Role> roles,
		String passwordHash) {
}