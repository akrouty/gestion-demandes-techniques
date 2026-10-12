package com.akrouty.gestiondemandes.request.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

/** Contexte complet avec le vrai adaptateur et sans configuration IA. */
@SpringBootTest(properties = {"app.ai.groq.api-key=", "app.ai.groq.timeout="})
class AnalyseIaConfigurationAbsenteApiTest extends WorkflowApiTestSupport {
    @Test
    void demarrage_sans_configuration_ia_et_metier_manuel_disponible() throws Exception {
        appel(post("/api/v1/demandes/analyse-ia").contentType(MediaType.APPLICATION_JSON)
                .content("{\"titre\":\"Calcul\",\"description\":\"Dimensionner\"}"), rtToken)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ANALYSE_IA_INDISPONIBLE"));
        String reference = nouvelle();
        appel(put("/api/v1/demandes/" + reference + "/qualification")
                .contentType(MediaType.APPLICATION_JSON).content("{\"categorie\":\"AUTRE\",\"priorite\":\"BASSE\"}"), rtToken)
                .andExpect(status().isOk());
    }
}
