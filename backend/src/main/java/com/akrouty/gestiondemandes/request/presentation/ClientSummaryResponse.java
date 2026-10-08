package com.akrouty.gestiondemandes.request.presentation;

/** DTO de lecture d'un client (API-CONTRACT-V1 §4.2). */
public record ClientSummaryResponse(Long id, String nom, String email, String telephone) {
}
