package com.akrouty.gestiondemandes.request.presentation;

import com.akrouty.gestiondemandes.identity.domain.Role;
import java.util.Set;

/**
 * Utilisateur résumé tel qu'inclus dans les réponses de demandes — équivalent
 * conceptuel de {@code UtilisateurSummaryResponse} (API-CONTRACT-V1 §4.3),
 * défini LOCALLY pour ne pas coupler {@code request.presentation} à
 * {@code identity.presentation}. Aucun credential.
 */
public record UtilisateurResumeResponse(Long id, String nom, String email, boolean actif, Set<Role> roles) {
}
