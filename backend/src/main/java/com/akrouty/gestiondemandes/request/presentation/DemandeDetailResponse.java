package com.akrouty.gestiondemandes.request.presentation;

import com.akrouty.gestiondemandes.request.domain.Categorie;
import com.akrouty.gestiondemandes.request.domain.Priorite;
import com.akrouty.gestiondemandes.request.domain.StatutDemande;
import java.time.Instant;

/**
 * Détail d'une demande (API-CONTRACT-V1 §4.1). L'historique complet n'est
 * pas inclus.
 */
public record DemandeDetailResponse(
		String reference,
		String titre,
		String description,
		Categorie categorie,
		Priorite priorite,
		StatutDemande statut,
		ClientSummaryResponse client,
		UtilisateurResumeResponse createur,
		UtilisateurResumeResponse agentAffecte,
		String descriptionTraitement,
		String solution,
		String motifAnnulation,
		Instant dateCreation,
		Instant dateModification,
		Instant dateResolution,
		Instant dateCloture,
		Instant dateAnnulation) {
}
