package com.akrouty.gestiondemandes.request.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.akrouty.gestiondemandes.identity.domain.Role;
import com.akrouty.gestiondemandes.identity.domain.Utilisateur;
import com.akrouty.gestiondemandes.request.domain.DemandeTechnique;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** API réelle, JWT obtenus par login, aucune transaction englobante de test.
 * Chaque relecture observe les transactions terminées du service. */
abstract class WorkflowApiTestSupport extends DemandeApiTestSupport {
    protected Utilisateur rt, agentA, agentB, admin;
    protected String rtToken, aToken, bToken, adminToken;
    protected Long client;

    @BeforeEach
    void preparerWorkflow() {
        rt = creerUtilisateur("Responsable", "rt@example.com", true, Role.RESPONSABLE_TECHNIQUE);
        agentA = creerUtilisateur("Agent A", "a@example.com", true, Role.AGENT_TECHNIQUE);
        agentB = creerUtilisateur("Agent B", "b@example.com", true, Role.AGENT_TECHNIQUE);
        admin = creerUtilisateur("Admin", "admin@example.com", true, Role.ADMINISTRATEUR);
        rtToken = jeton(rt);
        aToken = jeton(agentA);
        bToken = jeton(agentB);
        adminToken = jeton(admin);
        client = creerClient("Client Alpha", "contact@example.com", "+216 123456");
    }

    protected ResultActions appel(MockHttpServletRequestBuilder requete, String token) throws Exception {
        return mockMvc.perform(requete.header("Authorization", BEARER_PREFIX + token));
    }

    protected ResultActions action(String ref, String action, String token) throws Exception {
        return appel(post("/api/v1/demandes/" + ref + "/" + action), token);
    }

    protected ResultActions affectation(String ref, Long id, String token) throws Exception {
        return appel(put("/api/v1/demandes/" + ref + "/affectation")
                .contentType(MediaType.APPLICATION_JSON).content("{\"agentId\":" + id + "}"), token);
    }

    protected ResultActions traitement(String ref, String json, String token) throws Exception {
        return appel(patch("/api/v1/demandes/" + ref + "/traitement")
                .contentType(MediaType.APPLICATION_JSON).content(json), token);
    }

    protected ResultActions annulation(String ref, String json, String token) throws Exception {
        return appel(post("/api/v1/demandes/" + ref + "/annulation")
                .contentType(MediaType.APPLICATION_JSON).content(json), token);
    }

    protected String nouvelle() { return creerDemande(rtToken, client); }

    protected String dansEtat(String etat) throws Exception {
        String ref = nouvelle();
        if (etat.equals("ANNULEE")) {
            annulation(ref, "{\"motif\":\"Sans objet\"}", rtToken).andExpect(status().isOk());
            return ref;
        }
        if (etat.equals("NOUVELLE")) return ref;
        affecter(rtToken, ref, agentA.getId());
        if (etat.equals("ASSIGNEE")) return ref;
        demarrerTraitement(aToken, ref);
        if (etat.equals("EN_COURS")) return ref;
        majTraitement(aToken, ref, "{\"descriptionTraitement\":\"Diagnostic\",\"solution\":\"Correctif\"}");
        resoudre(aToken, ref);
        if (etat.equals("CLOTUREE")) cloturer(rtToken, ref);
        return ref;
    }

    protected DemandeTechnique relire(String ref) {
        return demandeRepository.findByReference(ref).orElseThrow();
    }

    protected List<String> references(ResultActions resultat) throws Exception {
        return JsonPath.read(corps(resultat.andExpect(status().isOk()).andReturn()), "$.items[*].reference");
    }

    protected void evenement(String ref, String type, Long auteur, String ancienne, String nouvelle) {
        var lignes = jdbcTemplate.queryForList("""
                SELECT h.* FROM historique_demande h JOIN demande_technique d ON d.id = h.demande_id
                WHERE d.reference = ? AND h.type_evenement = ? ORDER BY h.id
                """, ref, type);
        assertThat(lignes).isNotEmpty();
        var ligne = lignes.getLast();
        assertThat(((Number) ligne.get("demande_id")).longValue()).isEqualTo(relire(ref).getId());
        assertThat(((Number) ligne.get("auteur_id")).longValue()).isEqualTo(auteur);
        assertThat(ligne.get("date_evenement")).isNotNull();
        // getString rend les colonnes TEXT indépendamment de leur représentation JDBC.
        var valeurs = jdbcTemplate.queryForObject("""
                SELECT ancienne_valeur, nouvelle_valeur FROM historique_demande WHERE id = ?
                """, (rs, row) -> new String[] {rs.getString(1), rs.getString(2)}, ligne.get("id"));
        assertThat(valeurs).containsExactly(ancienne, nouvelle);
    }

    protected void inchange(String ref, Instant date, int nombre) {
        assertThat(relire(ref).getDateModification()).isEqualTo(date);
        assertThat(compterEvenements(ref)).isEqualTo(nombre);
    }
}
