package com.akrouty.gestiondemandes.identity.presentation;

import com.akrouty.gestiondemandes.identity.domain.Role;
import java.util.Set;

/** DTO de détail (API-CONTRACT-V1) : aucun credential, aucun détail de sécurité. */
public record UtilisateurDetailResponse(Long id, String nom, String email, boolean actif, Set<Role> roles) {
}