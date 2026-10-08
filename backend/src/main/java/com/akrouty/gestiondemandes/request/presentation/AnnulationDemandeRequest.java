package com.akrouty.gestiondemandes.request.presentation;

/** DTO d'annulation (API-CONTRACT-V1) : motif obligatoire non vide. */
public record AnnulationDemandeRequest(@jakarta.validation.constraints.NotBlank String motif) {
}
