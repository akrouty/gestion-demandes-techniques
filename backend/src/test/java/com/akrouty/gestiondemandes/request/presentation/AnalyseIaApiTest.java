package com.akrouty.gestiondemandes.request.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.akrouty.gestiondemandes.request.application.AnalyseIaIndisponibleException;
import com.akrouty.gestiondemandes.request.application.AnalyseIaPort;
import com.akrouty.gestiondemandes.request.application.AnalyseIaResultat;
import com.akrouty.gestiondemandes.request.domain.Categorie;
import com.akrouty.gestiondemandes.request.domain.Priorite;
import com.jayway.jsonpath.JsonPath;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class AnalyseIaApiTest extends WorkflowApiTestSupport {
    @MockitoBean
    private AnalyseIaPort port;
    private static final String CORPS = "{\"titre\":\"Calcul\",\"description\":\"Dimensionner le support\"}";

    @BeforeEach
    void resultatPort() {
        when(port.analyser("Calcul", "Dimensionner le support"))
                .thenReturn(new AnalyseIaResultat(Categorie.NOTE_CALCUL, Priorite.HAUTE));
    }

    @Test
    void rt_recoit_exactement_les_deux_propositions_sans_persistance() throws Exception {
        long demandesAvant = demandeRepository.count();
        long clientsAvant = clientRepository.count();
        int historiqueAvant = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM historique_demande", Integer.class);
        String reponse = corps(appel(post("/api/v1/demandes/analyse-ia")
                .contentType(MediaType.APPLICATION_JSON).content(CORPS), rtToken)
                .andExpect(status().isOk()).andReturn());
        Map<String,Object> champs = JsonPath.read(reponse, "$");
        assertThat(champs).containsOnlyKeys("categorieProposee", "prioriteProposee")
                .containsEntry("categorieProposee", "NOTE_CALCUL").containsEntry("prioriteProposee", "HAUTE");
        verify(port).analyser("Calcul", "Dimensionner le support");
        verifyNoMoreInteractions(port);
        assertThat(demandeRepository.count()).isEqualTo(demandesAvant);
        assertThat(clientRepository.count()).isEqualTo(clientsAvant);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM historique_demande", Integer.class))
                .isEqualTo(historiqueAvant);
    }

    @Test
    void at_et_administrateur_seul_refuses() throws Exception {
        for (String token : new String[]{aToken, adminToken}) {
            appel(post("/api/v1/demandes/analyse-ia").contentType(MediaType.APPLICATION_JSON).content(CORPS), token)
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(port);
    }

    @Test
    void non_authentifie_401() throws Exception {
        mockMvc.perform(post("/api/v1/demandes/analyse-ia").contentType(MediaType.APPLICATION_JSON).content(CORPS))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(port);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"titre\":\"\",\"description\":\"texte\"}",
            "{\"titre\":\"  \",\"description\":\"texte\"}",
            "{\"titre\":\"titre\",\"description\":\"\"}",
            "{\"titre\":\"titre\",\"description\":\"  \"}", "{}", "invalid-json"})
    void validation_http_400_sans_appel_port(String corps) throws Exception {
        appel(post("/api/v1/demandes/analyse-ia").contentType(MediaType.APPLICATION_JSON).content(corps), rtToken)
                .andExpect(status().isBadRequest());
        verifyNoInteractions(port);
    }

    @Test
    void panne_503_sans_details_et_qualification_manuelle_possible() throws Exception {
        String reference = nouvelle();
        int evenements = compterEvenements(reference);
        when(port.analyser(anyString(), anyString())).thenThrow(new AnalyseIaIndisponibleException());
        String erreur = corps(appel(post("/api/v1/demandes/analyse-ia")
                .contentType(MediaType.APPLICATION_JSON).content(CORPS), rtToken)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ANALYSE_IA_INDISPONIBLE")).andReturn());
        Map<String,Object> champs = JsonPath.read(erreur, "$");
        assertThat(champs).containsOnlyKeys("code", "message");
        assertThat(erreur).doesNotContain("Groq", "groq.com", "Authorization", "Exception", "stackTrace", "GROQ_API_KEY");
        assertThat(compterEvenements(reference)).isEqualTo(evenements);
        appel(put("/api/v1/demandes/" + reference + "/qualification")
                .contentType(MediaType.APPLICATION_JSON).content("{\"categorie\":\"AUTRE\",\"priorite\":\"BASSE\"}"), rtToken)
                .andExpect(status().isOk()).andExpect(jsonPath("$.categorie").value("AUTRE"))
                .andExpect(jsonPath("$.priorite").value("BASSE"));
        assertThat(compterEvenements(reference)).isGreaterThan(evenements);
    }
}
