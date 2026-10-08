package com.akrouty.gestiondemandes.request.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DemandeTraitementApiTest extends WorkflowApiTestSupport {
    @Test
    void demarrage_par_agent_affecte() throws Exception {
        String ref = dansEtat("ASSIGNEE");
        var avant = relire(ref).getDateModification();
        action(ref, "demarrage-traitement", aToken).andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EN_COURS"));
        assertThat(relire(ref).getDateModification()).isAfter(avant);
        evenement(ref, "TRAITEMENT_DEMARRE", agentA.getId(), "ASSIGNEE", "EN_COURS");
    }

    @Test
    void demarrage_et_traitement_par_autre_agent_refuses() throws Exception {
        String ref = dansEtat("ASSIGNEE");
        action(ref, "demarrage-traitement", bToken).andExpect(status().isForbidden());
        demarrerTraitement(aToken, ref);
        traitement(ref, "{\"solution\":\"Correctif\"}", bToken).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"EN_COURS", "RESOLUE", "CLOTUREE"})
    void demarrage_etat_incompatible(String etat) throws Exception {
        action(dansEtat(etat), "demarrage-traitement", aToken).andExpect(status().isConflict());
    }

    @Test
    void patch_partiel_et_complet_conservent_champs_absents() throws Exception {
        String ref = dansEtat("EN_COURS");
        traitement(ref, "{\"descriptionTraitement\":\"Diagnostic\"}", aToken)
                .andExpect(status().isOk()).andExpect(jsonPath("$.descriptionTraitement").value("Diagnostic"))
                .andExpect(jsonPath("$.solution").doesNotExist());
        traitement(ref, "{\"solution\":\"Correctif\"}", aToken)
                .andExpect(status().isOk()).andExpect(jsonPath("$.descriptionTraitement").value("Diagnostic"))
                .andExpect(jsonPath("$.solution").value("Correctif"));
        traitement(ref, "{\"descriptionTraitement\":\"Diagnostic final\",\"solution\":\"Correctif final\"}", aToken)
                .andExpect(status().isOk()).andExpect(jsonPath("$.descriptionTraitement").value("Diagnostic final"))
                .andExpect(jsonPath("$.solution").value("Correctif final"));
        traitement(ref, "{\"descriptionTraitement\":\"Suite\"}", aToken)
                .andExpect(status().isOk()).andExpect(jsonPath("$.solution").value("Correctif final"));
        evenement(ref, "DESCRIPTION_TRAITEMENT_MODIFIEE", agentA.getId(), "Diagnostic final", "Suite");
        evenement(ref, "SOLUTION_MODIFIEE", agentA.getId(), "Correctif", "Correctif final");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"solution\":null}", "{\"solution\":\"   \"}",
            "{\"solution\":\"\"}", "{\"descriptionTraitement\":null}",
            "{\"descriptionTraitement\":\"   \"}", "{\"descriptionTraitement\":\"\"}",
            "{\"descriptionTraitement\":\"Valide\",\"solution\":null}",
            "{\"descriptionTraitement\":\"Valide\",\"solution\":null,\"solutionFournie\":false}",
            "{\"solution\":\"Valide\",\"descriptionTraitement\":null,\"descriptionTraitementFournie\":false}"})
    void patch_invalide_400_sans_effet(String json) throws Exception {
        String ref = dansEtat("EN_COURS");
        var avant = relire(ref).getDateModification();
        int nombre = compterEvenements(ref);
        traitement(ref, json, aToken).andExpect(status().isBadRequest());
        inchange(ref, avant, nombre);
        assertThat(relire(ref).getDescriptionTraitement()).isNull();
        assertThat(relire(ref).getSolution()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ASSIGNEE", "RESOLUE", "CLOTUREE"})
    void patch_hors_en_cours_409(String etat) throws Exception {
        traitement(dansEtat(etat), "{\"solution\":\"Correctif\"}", aToken).andExpect(status().isConflict());
    }

    @Test
    void valeurs_identiques_sans_historique_ni_date_artificiels() throws Exception {
        String ref = dansEtat("EN_COURS");
        String json = "{\"descriptionTraitement\":\"Diagnostic\",\"solution\":\"Correctif\"}";
        majTraitement(aToken, ref, json);
        var avant = relire(ref).getDateModification();
        int nombre = compterEvenements(ref);
        traitement(ref, json, aToken).andExpect(status().isOk());
        inchange(ref, avant, nombre);
    }
}
