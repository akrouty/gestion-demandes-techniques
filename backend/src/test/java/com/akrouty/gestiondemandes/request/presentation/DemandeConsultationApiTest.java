package com.akrouty.gestiondemandes.request.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.akrouty.gestiondemandes.identity.domain.Role;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;

class DemandeConsultationApiTest extends WorkflowApiTestSupport {
    @org.springframework.beans.factory.annotation.Autowired
    private jakarta.persistence.EntityManagerFactory entityManagerFactory;
    private String premiere, deuxieme, troisieme;
    private Long autreClient;

    @BeforeEach
    void preparerDemandes() throws Exception {
        premiere = nouvelle();
        affecter(rtToken, premiere, agentA.getId());
        autreClient = creerClient("Client Beta", "beta@example.com", "555");
        deuxieme = creerDemande(rtToken, autreClient);
        affecter(rtToken, deuxieme, agentB.getId());
        appel(put("/api/v1/demandes/" + deuxieme + "/qualification")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"categorie\":\"AUTRE\",\"priorite\":\"BASSE\"}"), rtToken).andExpect(status().isOk());
        troisieme = JsonPath.read(corps(appel(post("/api/v1/demandes")
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"titre":"Inspection Pompe","description":"Fuite Hydraulique",
                 "categorie":"NOTE_CALCUL","priorite":"HAUTE","clientId":%d}
                """.formatted(client)), rtToken).andExpect(status().isCreated()).andReturn()), "$.reference");
    }

    @Test
    void perimetres_rt_at_et_filtre_agent_ne_s_elargissent_pas() throws Exception {
        assertThat(references(appel(get("/api/v1/demandes"), rtToken)))
                .containsExactlyInAnyOrder(premiere, deuxieme, troisieme);
        assertThat(references(appel(get("/api/v1/demandes"), aToken))).containsExactly(premiere);
        assertThat(references(appel(get("/api/v1/demandes").param("agentId", agentB.getId().toString()), aToken))).isEmpty();
        assertThat(references(appel(get("/api/v1/demandes").param("agentId", agentB.getId().toString()), rtToken)))
                .containsExactly(deuxieme);
        appel(get("/api/v1/demandes"), adminToken).andExpect(status().isForbidden());
    }

    @Test
    void detail_controle_agent_affecte_et_union_rt_at() throws Exception {
        for (String ref : List.of(premiere, deuxieme, troisieme))
            appel(get("/api/v1/demandes/" + ref), rtToken).andExpect(status().isOk())
                    .andExpect(jsonPath("$.historique").doesNotExist());
        appel(get("/api/v1/demandes/" + premiere), aToken).andExpect(status().isOk());
        appel(get("/api/v1/demandes/" + deuxieme), aToken).andExpect(status().isForbidden());
        appel(get("/api/v1/demandes/" + troisieme), aToken).andExpect(status().isForbidden());
        appel(get("/api/v1/demandes/" + premiere), adminToken).andExpect(status().isForbidden());
        var multi = creerUtilisateur("RT AT", "multi@example.com", true, Role.RESPONSABLE_TECHNIQUE, Role.AGENT_TECHNIQUE);
        String token = jeton(multi);
        assertThat(references(appel(get("/api/v1/demandes"), token)))
                .containsExactlyInAnyOrder(premiere, deuxieme, troisieme);
        appel(get("/api/v1/demandes/" + deuxieme), token).andExpect(status().isOk());
        // RT donne une lecture globale, mais ne supprime pas le contrôle d'affectation pour traiter.
        action(premiere, "demarrage-traitement", token).andExpect(status().isForbidden());
    }

    @Test
    void chaque_filtre_et_combinaison_sont_effectifs() throws Exception {
        assertThat(references(appel(get("/api/v1/demandes").param("statut", "NOUVELLE"), rtToken))).containsExactly(troisieme);
        assertThat(references(appel(get("/api/v1/demandes").param("priorite", "BASSE"), rtToken))).containsExactly(deuxieme);
        assertThat(references(appel(get("/api/v1/demandes").param("categorie", "AUTRE"), rtToken))).containsExactly(deuxieme);
        assertThat(references(appel(get("/api/v1/demandes").param("clientId", autreClient.toString()), rtToken))).containsExactly(deuxieme);
        assertThat(references(appel(get("/api/v1/demandes").param("agentId", agentA.getId().toString()), rtToken))).containsExactly(premiere);
        assertThat(references(appel(get("/api/v1/demandes").param("statut", "ASSIGNEE")
                .param("priorite", "HAUTE").param("categorie", "NOTE_CALCUL").param("clientId", client.toString())
                .param("agentId", agentA.getId().toString()).param("recherche", "MOTEUR"), rtToken))).containsExactly(premiere);
        assertThat(references(appel(get("/api/v1/demandes").param("statut", "NOUVELLE")
                .param("priorite", "BASSE"), rtToken))).isEmpty();
    }

    @Test
    void recherche_reference_titre_description_insensible_casse() throws Exception {
        for (String recherche : List.of(troisieme.toUpperCase(Locale.ROOT), "iNsPeCtIoN", "hYdRaUlIqUe"))
            assertThat(references(appel(get("/api/v1/demandes").param("recherche", recherche), rtToken))).containsExactly(troisieme);
    }

    @Test
    void pagination_tri_defaut_et_tri_explicite() throws Exception {
        assertThat(relire(troisieme).getDateCreation()).isAfter(relire(deuxieme).getDateCreation());
        assertThat(relire(deuxieme).getDateCreation()).isAfter(relire(premiere).getDateCreation());
        var page = appel(get("/api/v1/demandes").param("page", "0").param("size", "2"), rtToken)
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2)).andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        assertThat(references(page)).containsExactly(troisieme, deuxieme);
        assertThat(references(appel(get("/api/v1/demandes").param("page", "1").param("size", "2"), rtToken)))
                .containsExactly(premiere);
        assertThat(references(appel(get("/api/v1/demandes").param("sort", "dateCreation,asc"), rtToken)))
                .containsExactly(premiere, deuxieme, troisieme);
    }

    @Test
    void lecture_paginee_ne_multiplie_pas_les_requetes_par_client_ou_agent() throws Exception {
        var statistiques = entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        boolean actifAvant = statistiques.isStatisticsEnabled();
        statistiques.setStatisticsEnabled(true);
        try {
            statistiques.clear();
            appel(get("/api/v1/demandes").param("size", "1").param("sort", "dateCreation,asc"), rtToken)
                    .andExpect(status().isOk());
            long uneLigne = statistiques.getPrepareStatementCount();
            statistiques.clear();
            appel(get("/api/v1/demandes").param("size", "2").param("sort", "dateCreation,asc"), rtToken)
                    .andExpect(status().isOk()).andExpect(jsonPath("$.items[1].client.nom").value("Client Beta"))
                    .andExpect(jsonPath("$.items[1].agentAffecte.email").value(agentB.getEmail()));
            assertThat(uneLigne).isPositive();
            assertThat(statistiques.getPrepareStatementCount()).isEqualTo(uneLigne);
        } finally {
            statistiques.setStatisticsEnabled(actifAvant);
            statistiques.clear();
        }
    }

    @ParameterizedTest
    @CsvSource(value = {"page|-1", "page|abc", "size|0", "size|-1", "sort|inconnu,asc",
            "sort|dateCreation,wrong", "sort|,", "sort|dateCreation,", "sort|dateCreation,asc,extra",
            "statut|FAUX", "priorite|FAUX", "categorie|FAUX", "clientId|abc", "agentId|abc"}, delimiter = '|')
    void parametres_invalides_400(String nom, String valeur) throws Exception {
        var resultat = appel(get("/api/v1/demandes").param(nom, valeur), rtToken)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PARAMETRE_INVALIDE"));
        assertThat(corps(resultat.andReturn())).doesNotContain("Exception", "SELECT", "constraint", "stackTrace");
    }
}
