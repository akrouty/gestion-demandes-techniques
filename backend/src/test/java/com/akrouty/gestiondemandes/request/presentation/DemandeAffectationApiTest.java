package com.akrouty.gestiondemandes.request.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.akrouty.gestiondemandes.identity.domain.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DemandeAffectationApiTest extends WorkflowApiTestSupport {
    @Test
    void affectation_active_et_historique_persistes() throws Exception {
        String ref = nouvelle();
        var avant = relire(ref).getDateModification();
        affectation(ref, agentA.getId(), rtToken).andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ASSIGNEE"))
                .andExpect(jsonPath("$.agentAffecte.id").value(agentA.getId()));
        assertThat(relire(ref).getDateModification()).isAfter(avant);
        evenement(ref, "AFFECTATION", rt.getId(), null, agentA.getEmail());
    }

    @Test
    void agent_inexistant_404_sans_modification() throws Exception {
        String ref = nouvelle();
        affectation(ref, Long.MAX_VALUE, rtToken).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("UTILISATEUR_INTROUVABLE"));
        assertThat(typesEvenements(ref)).containsExactly("CREATION");
        assertThat(relire(ref).getAgentAffecte()).isNull();
    }

    @Test
    void cible_sans_role_at_ou_inactive_409() throws Exception {
        String ref = nouvelle();
        var inactif = creerUtilisateur("Inactif", "inactif@example.com", false, Role.AGENT_TECHNIQUE);
        for (Long id : new Long[] {rt.getId(), admin.getId(), inactif.getId()}) {
            affectation(ref, id, rtToken).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("AGENT_NON_AFFECTABLE"));
        }
        assertThat(typesEvenements(ref)).containsExactly("CREATION");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ASSIGNEE", "EN_COURS"})
    void reaffectation_vers_autre_agent_conserve_les_donnees(String etat) throws Exception {
        String ref = dansEtat(etat);
        if (etat.equals("EN_COURS"))
            majTraitement(aToken, ref, "{\"descriptionTraitement\":\"Diagnostic\",\"solution\":\"Correctif\"}");
        affectation(ref, agentB.getId(), rtToken).andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ASSIGNEE"))
                .andExpect(jsonPath("$.agentAffecte.id").value(agentB.getId()));
        if (etat.equals("EN_COURS")) {
            assertThat(relire(ref).getDescriptionTraitement()).isEqualTo("Diagnostic");
            assertThat(relire(ref).getSolution()).isEqualTo("Correctif");
        }
        evenement(ref, "REAFFECTATION", rt.getId(), agentA.getEmail(), agentB.getEmail());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ASSIGNEE", "EN_COURS"})
    void meme_agent_ne_change_ni_date_ni_historique(String etat) throws Exception {
        String ref = dansEtat(etat);
        var avant = relire(ref).getDateModification();
        int nombre = compterEvenements(ref);
        affectation(ref, agentA.getId(), rtToken).andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value(etat));
        inchange(ref, avant, nombre);
    }

    @ParameterizedTest
    @ValueSource(strings = {"RESOLUE", "CLOTUREE", "ANNULEE"})
    void etat_incompatible_409(String etat) throws Exception {
        affectation(dansEtat(etat), agentB.getId(), rtToken).andExpect(status().isConflict());
    }

    @Test
    void at_et_admin_seuls_refuses() throws Exception {
        String ref = nouvelle();
        affectation(ref, agentB.getId(), aToken).andExpect(status().isForbidden());
        affectation(ref, agentB.getId(), adminToken).andExpect(status().isForbidden());
    }
}
