package com.akrouty.gestiondemandes.request.presentation;

import com.akrouty.gestiondemandes.request.domain.Categorie;
import com.akrouty.gestiondemandes.request.domain.Priorite;

public record AnalyseIaResponse(Categorie categorieProposee, Priorite prioriteProposee) {
}
