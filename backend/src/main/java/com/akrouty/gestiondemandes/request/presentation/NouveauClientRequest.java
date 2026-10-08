package com.akrouty.gestiondemandes.request.presentation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Informations minimales d'un client créé pendant l'enregistrement d'une
 * demande (RM07). Email syntaxiquement valide mais NON unique (ADR-003).
 */
public record NouveauClientRequest(
		@NotBlank String nom,
		@NotBlank @Email String email,
		@NotBlank String telephone) {
}
