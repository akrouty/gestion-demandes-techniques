package com.akrouty.gestiondemandes.request.presentation;

import com.akrouty.gestiondemandes.request.domain.Categorie;
import com.akrouty.gestiondemandes.request.domain.Priorite;
import com.akrouty.gestiondemandes.request.domain.StatutDemande;
import java.time.Instant;

/** Résumé de demande pour la liste paginée (API-CONTRACT-V1 §4.1). */
public record DemandeSummaryResponse(
		String reference,
		String titre,
		Categorie categorie,
		Priorite priorite,
		StatutDemande statut,
		ClientSummaryResponse client,
		UtilisateurResumeResponse agentAffecte,
		Instant dateCreation,
		Instant dateModification) {
}
