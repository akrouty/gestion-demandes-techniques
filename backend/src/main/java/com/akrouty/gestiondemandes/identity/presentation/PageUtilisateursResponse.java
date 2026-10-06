package com.akrouty.gestiondemandes.identity.presentation;

import java.util.List;

/**
 * Réponse paginée de {@code GET /api/v1/utilisateurs} (API-CONTRACT-V1) :
 * le contrat d'API est respecté, l'objet {@code Page} interne de Spring n'est
 * pas exposé tel quel.
 */
public record PageUtilisateursResponse(
		List<UtilisateurSummaryResponse> items,
		int page,
		int size,
		long totalElements,
		int totalPages) {
}