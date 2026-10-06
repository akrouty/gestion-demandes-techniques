package com.akrouty.gestiondemandes.identity.presentation;

import com.akrouty.gestiondemandes.identity.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

/**
 * DTO de création (API-CONTRACT-V1). {@code actif} est explicite et obligatoire,
 * {@code password} est une donnée d'entrée uniquement (jamais retourné),
 * {@code rolesMetier} n'accepte que RT et AT.
 */
public record CreationUtilisateurRequest(
		@NotBlank String nom,
		@NotBlank @Email String email,
		@NotNull Boolean actif,
		Set<Role> rolesMetier,
		@NotBlank String password) {
}