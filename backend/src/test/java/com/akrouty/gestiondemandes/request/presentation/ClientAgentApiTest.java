package com.akrouty.gestiondemandes.request.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.akrouty.gestiondemandes.identity.domain.Role;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.bind.annotation.RequestMethod;

class ClientAgentApiTest extends WorkflowApiTestSupport {
    @Autowired
    private RequestMappingHandlerMapping mappings;

    @ParameterizedTest
    @ValueSource(strings = {"aLpHa", "CONTACT@EXAMPLE", "123456"})
    void recherche_client_nom_email_telephone(String recherche) throws Exception {
        creerClient("Beta", "beta@example.com", "999");
        appel(get("/api/v1/clients").param("recherche", recherche), rtToken)
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].id").value(client));
    }

    @Test
    void clients_pagination_et_tri() throws Exception {
        creerClient("Zulu", "z@example.com", "999");
        creerClient("Beta", "b@example.com", "888");
        appel(get("/api/v1/clients").param("page", "1").param("size", "2").param("sort", "nom,asc"), rtToken)
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2)).andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].nom").value("Zulu"));
    }

    @ParameterizedTest
    @CsvSource(value = {"page|-1", "size|0", "page|abc", "sort|inconnu,asc", "sort|nom,wrong",
            "sort|,", "sort|nom,", "sort|nom,asc,extra"}, delimiter = '|')
    void clients_parametres_invalides(String nom, String valeur) throws Exception {
        appel(get("/api/v1/clients").param(nom, valeur), rtToken).andExpect(status().isBadRequest());
    }

    @Test
    void clients_et_agents_reserves_rt() throws Exception {
        for (String route : List.of("clients", "agents")) {
            appel(get("/api/v1/" + route), aToken).andExpect(status().isForbidden());
            appel(get("/api/v1/" + route), adminToken).andExpect(status().isForbidden());
        }
    }

    @Test
    void aucun_crud_client_ni_endpoint_ia_historique() {
        var routes = mappings.getHandlerMethods().keySet();
        assertThat(routes.stream().filter(r -> r.getPatternValues().stream().anyMatch(p -> p.startsWith("/api/v1/clients"))))
                .allSatisfy(r -> assertThat(r.getMethodsCondition().getMethods()).containsExactly(RequestMethod.GET));
        assertThat(routes.stream().flatMap(r -> r.getPatternValues().stream()))
                .noneMatch(p -> p.contains("analyse-ia") || p.contains("historique"));
    }

    @Test
    void agents_actifs_at_seulement_sans_credentials() throws Exception {
        creerUtilisateur("Inactif", "inactive@example.com", false, Role.AGENT_TECHNIQUE);
        var multi = creerUtilisateur("Multi", "multi@example.com", true, Role.RESPONSABLE_TECHNIQUE, Role.AGENT_TECHNIQUE);
        String json = corps(appel(get("/api/v1/agents"), rtToken).andExpect(status().isOk()).andReturn());
        List<Integer> ids = JsonPath.read(json, "$[*].id");
        assertThat(ids).containsExactlyInAnyOrder(agentA.getId().intValue(), agentB.getId().intValue(), multi.getId().intValue());
        List<java.util.Map<String, Object>> agents = JsonPath.read(json, "$");
        assertThat(agents).allSatisfy(a -> {
            assertThat(a).containsOnlyKeys("id", "nom", "email", "actif");
            assertThat(a.get("actif")).isEqualTo(true);
            assertThat(a.get("nom")).isNotNull();
            assertThat(a.get("email")).isNotNull();
        });
        assertThat(json).doesNotContain("password", "passwordHash");
    }
}
