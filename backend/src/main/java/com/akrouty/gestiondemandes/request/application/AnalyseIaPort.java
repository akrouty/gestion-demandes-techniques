package com.akrouty.gestiondemandes.request.application;

/** Frontiere IA limitee a titre/description, sans type fournisseur (ADR-002). */
public interface AnalyseIaPort {
    AnalyseIaResultat analyser(String titre, String description);
}
