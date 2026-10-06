package com.akrouty.gestiondemandes.security.presentation;

import jakarta.validation.constraints.NotBlank;

/**
 * Entrée de {@code POST /api/v1/auth/login} (API-CONTRACT-V1).
 * L'email est normalisé par Identity avant recherche (une seule stratégie).
 */
public record LoginRequest(@NotBlank String email, @NotBlank String password) {
}