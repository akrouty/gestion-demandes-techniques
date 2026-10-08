package com.akrouty.gestiondemandes.request.presentation;

import jakarta.validation.constraints.NotNull;

/** DTO d'affectation (API-CONTRACT-V1) : cible un utilisateur actif avec AGENT_TECHNIQUE. */
public record AffectationDemandeRequest(@NotNull Long agentId) {
}
