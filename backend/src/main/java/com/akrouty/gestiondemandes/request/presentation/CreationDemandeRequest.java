package com.akrouty.gestiondemandes.request.presentation;

import com.akrouty.gestiondemandes.request.domain.Categorie;
import com.akrouty.gestiondemandes.request.domain.Priorite;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de création (API-CONTRACT-V1). Aucun champ de référence, statut,
 * créateur ou dates : tout est géré par le serveur.
 *
 * <p>Exactement un des champs {@code clientId} ou {@code nouveauClient}
 * doit être fourni (contrôlé côté métier → {@code 400}).</p>
 */
public record CreationDemandeRequest(
		@NotBlank String titre,
		@NotBlank String description,
		@NotNull Categorie categorie,
		@NotNull Priorite priorite,
		Long clientId,
		@Valid NouveauClientRequest nouveauClient) {
}
