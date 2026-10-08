package com.akrouty.gestiondemandes.request.application;

import com.akrouty.gestiondemandes.request.domain.Categorie;
import com.akrouty.gestiondemandes.request.domain.Priorite;

/**
 * Commande de création d'une demande. Le client REST ne fournit ni référence,
 * ni statut, ni créateur, ni dates (gérés par le serveur).
 *
 * @param clientId      présent si un client existant est choisi (sinon {@code null})
 * @param nouveauClient présent si un client est créé pendant l'enregistrement (sinon {@code null})
 */
public record CreationDemandeCommand(
		String titre,
		String description,
		Categorie categorie,
		Priorite priorite,
		Long clientId,
		NouveauClientCommand nouveauClient) {

	/** Informations minimales d'un nouveau client (RM07), sans déduplication (RM08). */
	public record NouveauClientCommand(String nom, String email, String telephone) {
	}
}
