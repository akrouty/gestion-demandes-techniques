package com.akrouty.gestiondemandes.request.application;

/** Erreur generique sans cause fournisseur, corps HTTP ni secret. */
public class AnalyseIaIndisponibleException extends RuntimeException {
    public AnalyseIaIndisponibleException() {
        super("Analyse IA temporairement indisponible.");
    }
}
