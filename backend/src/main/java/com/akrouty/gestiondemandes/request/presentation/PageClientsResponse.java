package com.akrouty.gestiondemandes.request.presentation;

import java.util.List;

/** Réponse paginée de {@code GET /api/v1/clients} (API-CONTRACT-V1 §9.1). */
public record PageClientsResponse(
		List<ClientSummaryResponse> items,
		int page,
		int size,
		long totalElements,
		int totalPages) {
}
