package com.akrouty.gestiondemandes.request.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DemandeAnnulationApiTest extends WorkflowApiTestSupport {
    @ParameterizedTest
    @ValueSource(strings = {"NOUVELLE", "ASSIGNEE", "EN_COURS"})
    void annulation_depuis_chaque_etat_autorise(String etat) throws Exception {
        String ref = dansEtat(etat);
        var avant = relire(ref).getDateModification();
        annulation(ref, "{\"motif\":\"Sans objet\"}", rtToken).andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ANNULEE"))
                .andExpect(jsonPath("$.motifAnnulation").value("Sans objet"));
        var demande = relire(ref);
        assertThat(demande.getDateAnnulation()).isAfter(avant).isEqualTo(demande.getDateModification());
        assertThat(demande.getMotifAnnulation()).isEqualTo("Sans objet");
        evenement(ref, "ANNULATION", rt.getId(), etat, "Sans objet");
    }

    @ParameterizedTest
    @ValueSource(strings = {"RESOLUE", "CLOTUREE", "ANNULEE"})
    void annulation_depuis_etat_interdit(String etat) throws Exception {
        String ref = dansEtat(etat);
        var avant = relire(ref).getDateModification();
        int nombre = compterEvenements(ref);
        annulation(ref, "{\"motif\":\"Sans objet\"}", rtToken).andExpect(status().isConflict());
        inchange(ref, avant, nombre);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"motif\":null}", "{\"motif\":\"   \"}"})
    void motif_obligatoire(String json) throws Exception {
        annulation(nouvelle(), json, rtToken).andExpect(status().isBadRequest());
    }

    @Test
    void agent_ne_peut_pas_annuler() throws Exception {
        annulation(nouvelle(), "{\"motif\":\"Sans objet\"}", aToken).andExpect(status().isForbidden());
    }
}
