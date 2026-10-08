package com.akrouty.gestiondemandes.request.presentation;

/**
 * Agent affectable (API-CONTRACT-V1 §4.3) : uniquement les utilisateurs
 * actifs ayant le rôle {@code AGENT_TECHNIQUE}. Aucun credential.
 */
public record AgentAssignableResponse(Long id, String nom, String email, boolean actif) {
}
