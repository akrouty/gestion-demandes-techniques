package com.akrouty.gestiondemandes.request.presentation;

import com.akrouty.gestiondemandes.request.domain.Categorie;
import com.akrouty.gestiondemandes.request.domain.Priorite;
import jakarta.validation.constraints.NotNull;

/** DTO de qualification (API-CONTRACT-V1) : aucun champ de statut. */
public record QualificationDemandeRequest(
		@NotNull Categorie categorie,
		@NotNull Priorite priorite) {
}
