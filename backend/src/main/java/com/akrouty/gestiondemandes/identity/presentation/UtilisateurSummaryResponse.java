package com.akrouty.gestiondemandes.identity.presentation;

import com.akrouty.gestiondemandes.identity.domain.Role;
import java.util.Set;

/** DTO de lecture (API-CONTRACT-V1) : aucun credential, aucun détail de sécurité. */
public record UtilisateurSummaryResponse(Long id, String nom, String email, boolean actif, Set<Role> roles) {
}