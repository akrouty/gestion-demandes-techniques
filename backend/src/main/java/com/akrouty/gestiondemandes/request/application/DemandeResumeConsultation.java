package com.akrouty.gestiondemandes.request.application;

import com.akrouty.gestiondemandes.identity.application.UtilisateurConsultation;
import com.akrouty.gestiondemandes.request.domain.Categorie;
import com.akrouty.gestiondemandes.request.domain.Priorite;
import com.akrouty.gestiondemandes.request.domain.StatutDemande;
import java.time.Instant;

/**
 * Projection non modifiable du RÉSUMÉ d'une demande pour la liste paginée
 * (consultation globale RT ou périmètre AT). Même justification que
 * {@link DemandeConsultation} : transaction + open-in-view=false + DTO REST
 * séparé. Elle ne contient ni description, ni traitement, ni historique.
 *
 * @param agentAffecte {@code null} tant qu'aucun Agent n'est affecté
 */
public record DemandeResumeConsultation(
		String reference,
		String titre,
		Categorie categorie,
		Priorite priorite,
		StatutDemande statut,
		ClientConsultation client,
		UtilisateurConsultation agentAffecte,
		Instant dateCreation,
		Instant dateModification) {
}