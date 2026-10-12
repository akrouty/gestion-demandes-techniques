package com.akrouty.gestiondemandes.request.application;

import org.springframework.stereotype.Service;

/** Orchestration sans transaction, persistance ou regle de classification. */
@Service
public class AnalyseIaService {
    private final AnalyseIaPort port;

    public AnalyseIaService(AnalyseIaPort port) {
        this.port = port;
    }

    public AnalyseIaResultat analyser(String titre, String description) {
        AnalyseIaResultat resultat = port.analyser(titre, description);
        if (resultat == null || resultat.categorieProposee() == null || resultat.prioriteProposee() == null) {
            throw new AnalyseIaIndisponibleException();
        }
        return resultat;
    }
}
