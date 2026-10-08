package com.akrouty.gestiondemandes.request.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DemandeResolutionApiTest extends WorkflowApiTestSupport {
    @Test
    void resolution_sans_solution_ne_laisse_aucun_etat_partiel_en_base() throws Exception {
        String ref = dansEtat("EN_COURS");
        var avant = relire(ref).getDateModification();
        int nombre = compterEvenements(ref);
        action(ref, "resolution", aToken).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SOLUTION_REQUISE"));
        // Pas de @Transactional sur ce test : lecture SQL après rollback de la requête.
        var etat = jdbcTemplate.queryForMap("SELECT statut, date_resolution FROM demande_technique WHERE reference = ?", ref);
        assertThat(etat.get("statut")).isEqualTo("EN_COURS");
        assertThat(etat.get("date_resolution")).isNull();
        inchange(ref, avant, nombre);
    }

    @Test
    void resolution_avec_solution_dates_et_evenement() throws Exception {
        String ref = dansEtat("EN_COURS");
        majTraitement(aToken, ref, "{\"solution\":\"Correctif\"}");
        var avant = relire(ref).getDateModification();
        action(ref, "resolution", aToken).andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("RESOLUE"))
                .andExpect(jsonPath("$.dateResolution").isNotEmpty());
        var demande = relire(ref);
        assertThat(demande.getDateResolution()).isAfter(avant).isEqualTo(demande.getDateModification());
        evenement(ref, "RESOLUTION", agentA.getId(), "EN_COURS", "RESOLUE");
    }

    @Test
    void resolution_interdite_aux_non_affectes_et_rt_sans_at() throws Exception {
        String ref = dansEtat("EN_COURS");
        majTraitement(aToken, ref, "{\"solution\":\"Correctif\"}");
        for (String token : new String[] {bToken, rtToken, adminToken})
            action(ref, "resolution", token).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ASSIGNEE", "RESOLUE", "CLOTUREE"})
    void resolution_etat_incompatible_409(String etat) throws Exception {
        action(dansEtat(etat), "resolution", aToken).andExpect(status().isConflict());
    }

    @Test
    void refus_conserve_traitement_solution_et_ancienne_resolution() throws Exception {
        String ref = dansEtat("RESOLUE");
        var avant = relire(ref).getDateModification();
        action(ref, "refus-resolution", rtToken).andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EN_COURS"))
                .andExpect(jsonPath("$.dateResolution").doesNotExist());
        var demande = relire(ref);
        assertThat(demande.getDateResolution()).isNull();
        assertThat(demande.getDateModification()).isAfter(avant);
        assertThat(demande.getDescriptionTraitement()).isEqualTo("Diagnostic");
        assertThat(demande.getSolution()).isEqualTo("Correctif");
        evenement(ref, "RESOLUTION", agentA.getId(), "EN_COURS", "RESOLUE");
        evenement(ref, "REFUS_RESOLUTION", rt.getId(), "RESOLUE", "EN_COURS");
    }

    @Test
    void cloture_dates_et_evenement_persistes() throws Exception {
        String ref = dansEtat("RESOLUE");
        var avant = relire(ref).getDateModification();
        action(ref, "cloture", rtToken).andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("CLOTUREE"));
        var demande = relire(ref);
        assertThat(demande.getDateCloture()).isAfter(avant).isEqualTo(demande.getDateModification());
        evenement(ref, "CLOTURE", rt.getId(), "RESOLUE", "CLOTUREE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"NOUVELLE", "ASSIGNEE", "EN_COURS", "CLOTUREE", "ANNULEE"})
    void refus_et_cloture_etat_incompatible_409(String etat) throws Exception {
        String ref = dansEtat(etat);
        action(ref, "refus-resolution", rtToken).andExpect(status().isConflict());
        action(ref, "cloture", rtToken).andExpect(status().isConflict());
    }

    @Test
    void refus_et_cloture_interdits_a_at() throws Exception {
        String ref = dansEtat("RESOLUE");
        action(ref, "refus-resolution", aToken).andExpect(status().isForbidden());
        action(ref, "cloture", aToken).andExpect(status().isForbidden());
    }
}
