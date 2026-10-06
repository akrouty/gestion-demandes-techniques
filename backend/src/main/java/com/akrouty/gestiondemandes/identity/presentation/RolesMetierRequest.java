package com.akrouty.gestiondemandes.identity.presentation;

import com.akrouty.gestiondemandes.identity.domain.Role;
import java.util.Set;

/**
 * DTO de remplacement des rôles métier (API-CONTRACT-V1) : remplace
 * uniquement RT et AT. La présence de {@code ADMINISTRATEUR} rend la
 * requête invalide ({@code 400}) ; un rôle {@code ADMINISTRATEUR} existant
 * est conservé par le domaine. Un ensemble vide est accepté.
 */
public record RolesMetierRequest(Set<Role> rolesMetier) {
}