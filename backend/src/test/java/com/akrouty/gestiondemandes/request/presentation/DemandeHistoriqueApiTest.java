package com.akrouty.gestiondemandes.request.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class DemandeHistoriqueApiTest extends WorkflowApiTestSupport {
    @Test
    void cycle_complet_historique_persiste_avec_auteurs_dates_et_valeurs() throws Exception {
        String ref = nouvelle();
        appel(put("/api/v1/demandes/" + ref + "/qualification").contentType(MediaType.APPLICATION_JSON)
                .content("{\"categorie\":\"AUTRE\",\"priorite\":\"BASSE\"}"), rtToken).andExpect(status().isOk());
        affecter(rtToken, ref, agentA.getId());
        affecter(rtToken, ref, agentB.getId());
        demarrerTraitement(bToken, ref);
        majTraitement(bToken, ref, "{\"descriptionTraitement\":\"Diagnostic\",\"solution\":\"Correctif\"}");
        resoudre(bToken, ref);
        action(ref, "refus-resolution", rtToken).andExpect(status().isOk());
        resoudre(bToken, ref);
        cloturer(rtToken, ref);
        assertThat(typesEvenements(ref)).containsExactly("CREATION", "CATEGORIE_MODIFIEE", "PRIORITE_MODIFIEE",
                "AFFECTATION", "REAFFECTATION", "TRAITEMENT_DEMARRE", "DESCRIPTION_TRAITEMENT_MODIFIEE",
                "SOLUTION_MODIFIEE", "RESOLUTION", "REFUS_RESOLUTION", "RESOLUTION", "CLOTURE");
        evenement(ref, "CREATION", rt.getId(), null, "NOUVELLE");
        evenement(ref, "CATEGORIE_MODIFIEE", rt.getId(), "NOTE_CALCUL", "AUTRE");
        evenement(ref, "PRIORITE_MODIFIEE", rt.getId(), "HAUTE", "BASSE");
        evenement(ref, "AFFECTATION", rt.getId(), null, agentA.getEmail());
        evenement(ref, "REAFFECTATION", rt.getId(), agentA.getEmail(), agentB.getEmail());
        evenement(ref, "TRAITEMENT_DEMARRE", agentB.getId(), "ASSIGNEE", "EN_COURS");
        evenement(ref, "DESCRIPTION_TRAITEMENT_MODIFIEE", agentB.getId(), null, "Diagnostic");
        evenement(ref, "SOLUTION_MODIFIEE", agentB.getId(), null, "Correctif");
        evenement(ref, "RESOLUTION", agentB.getId(), "EN_COURS", "RESOLUE");
        evenement(ref, "REFUS_RESOLUTION", rt.getId(), "RESOLUE", "EN_COURS");
        evenement(ref, "CLOTURE", rt.getId(), "RESOLUE", "CLOTUREE");
        // Chemin incompatible avec la clôture : autre demande pour l'annulation.
        String annulee = nouvelle();
        annulerViaApi(annulee, rtToken, "Sans objet");
        evenement(annulee, "ANNULATION", rt.getId(), "NOUVELLE", "Sans objet");
        assertThat(typesEvenements(annulee)).containsExactly("CREATION", "ANNULATION");
    }
}
