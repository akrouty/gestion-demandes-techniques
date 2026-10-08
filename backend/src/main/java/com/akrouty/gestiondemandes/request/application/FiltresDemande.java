package com.akrouty.gestiondemandes.request.application;

import com.akrouty.gestiondemandes.request.domain.Categorie;
import com.akrouty.gestiondemandes.request.domain.Priorite;
import com.akrouty.gestiondemandes.request.domain.StatutDemande;

/**
 * Filtres de consultation de la liste des demandes (API-CONTRACT-V1 §9.2).
 * Tous les champs sont optionnels ; aucun filtre ne peut élargir le périmètre
 * autorisé de l'utilisateur.
 */
public record FiltresDemande(
		StatutDemande statut,
		Priorite priorite,
		Categorie categorie,
		Long clientId,
		Long agentId,
		String recherche) {
}
