package com.akrouty.gestiondemandes.request.application;

import com.akrouty.gestiondemandes.request.domain.Categorie;
import com.akrouty.gestiondemandes.request.domain.Priorite;

/** Proposition temporaire, jamais persistee automatiquement. */
public record AnalyseIaResultat(Categorie categorieProposee, Priorite prioriteProposee) {
}
