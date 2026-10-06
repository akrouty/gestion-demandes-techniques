package com.akrouty.gestiondemandes.identity.presentation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO de modification des informations générales (API-CONTRACT-V1).
 * L'état {@code actif} et les rôles utilisent leurs opérations dédiées.
 */
public record ModificationUtilisateurRequest(
		@NotBlank String nom,
		@NotBlank @Email String email) {
}