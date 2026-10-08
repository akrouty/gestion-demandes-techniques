package com.akrouty.gestiondemandes.request.presentation;

import java.util.List;

/**
 * Réponse paginée de {@code GET /api/v1/demandes} (API-CONTRACT-V1 §9.1) :
 * le format interne de Spring n'est pas exposé tel quel.
 */
public record PageDemandesResponse(
		List<DemandeSummaryResponse> items,
		int page,
		int size,
		long totalElements,
		int totalPages) {
}
