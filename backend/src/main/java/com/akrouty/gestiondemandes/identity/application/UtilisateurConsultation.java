package com.akrouty.gestiondemandes.identity.application;

import com.akrouty.gestiondemandes.identity.domain.Role;
import java.util.Set;

/**
 * Projection non modifiable d'un utilisateur, matérialisée dans la transaction
 * du cas d'utilisation. Sert de responsabilité publique d'{@code identity}
 * vers les couches presentation et vers le module {@code security}.
 *
 * <p>Ne contient jamais {@code passwordHash} ni autre détail de sécurité.</p>
 */
public record UtilisateurConsultation(
		Long id,
		String nom,
		String email,
		boolean actif,
		Set<Role> roles) {
}