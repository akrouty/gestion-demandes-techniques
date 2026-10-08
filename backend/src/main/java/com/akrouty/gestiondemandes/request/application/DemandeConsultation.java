package com.akrouty.gestiondemandes.request.application;

import com.akrouty.gestiondemandes.identity.application.UtilisateurConsultation;
import com.akrouty.gestiondemandes.request.domain.Categorie;
import com.akrouty.gestiondemandes.request.domain.Priorite;
import com.akrouty.gestiondemandes.request.domain.StatutDemande;
import java.time.Instant;

/**
 * Projection non modifiable DU DÉTAIL d'une demande, matérialisée dans la
 * transaction du cas d'utilisation (open-in-view=false : aucune entité JPA
 * lazy n'est retournée vers la couche presentation).
 *
 * <p>L'historique n'en fait pas partie : le détail n'inclut jamais
 * automatiquement l'historique complet (API-CONTRACT-V1 §4.1).</p>
 *
 * @param createur     jamais null
 * @param agentAffecte {@code null} tant qu'aucun Agent n'est affecté
 */
public record DemandeConsultation(
		String reference,
		String titre,
		String description,
		Categorie categorie,
		Priorite priorite,
		StatutDemande statut,
		ClientConsultation client,
		UtilisateurConsultation createur,
		UtilisateurConsultation agentAffecte,
		String descriptionTraitement,
		String solution,
		String motifAnnulation,
		Instant dateCreation,
		Instant dateModification,
		Instant dateResolution,
		Instant dateCloture,
		Instant dateAnnulation) {
}
