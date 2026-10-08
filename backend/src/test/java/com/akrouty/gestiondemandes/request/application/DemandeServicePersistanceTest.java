package com.akrouty.gestiondemandes.request.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.akrouty.gestiondemandes.identity.domain.Role;
import com.akrouty.gestiondemandes.identity.domain.Utilisateur;
import com.akrouty.gestiondemandes.identity.persistence.UtilisateurRepository;
import com.akrouty.gestiondemandes.request.domain.*;
import com.akrouty.gestiondemandes.request.persistence.*;
import jakarta.persistence.EntityManager;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** Régression de la qualification d'une entité gérée avec cascade PERSIST seule. */
@SpringBootTest
@Transactional
class DemandeServicePersistanceTest {
    @Autowired private DemandeService service;
    @Autowired private UtilisateurRepository utilisateurs;
    @Autowired private ClientRepository clients;
    @Autowired private DemandeTechniqueRepository demandes;
    @Autowired private EntityManager entityManager;

    @Test
    void qualification_et_historique_sont_relus_apres_flush_et_clear() {
        var rt = utilisateurs.save(new Utilisateur("RT", "persistance@example.com", true, "hash",
                Set.of(Role.RESPONSABLE_TECHNIQUE)));
        var client = clients.save(new Client("C", "c@example.com", "123"));
        var creee = service.creer(rt.getId(), new CreationDemandeCommand(
                "Titre", "Description", Categorie.AUTRE, Priorite.BASSE, client.getId(), null));
        var modifiee = service.qualifier(rt.getId(), creee.reference(), Categorie.NOTE_CALCUL, Priorite.HAUTE);
        assertThat(modifiee.categorie()).isEqualTo(Categorie.NOTE_CALCUL);
        entityManager.flush();
        entityManager.clear();
        var relue = demandes.findByReference(creee.reference()).orElseThrow();
        assertThat(relue.getCategorie()).isEqualTo(Categorie.NOTE_CALCUL);
        assertThat(relue.getPriorite()).isEqualTo(Priorite.HAUTE);
        assertThat(relue.getHistorique()).extracting(HistoriqueDemande::getTypeEvenement)
                .containsExactly("CREATION", "CATEGORIE_MODIFIEE", "PRIORITE_MODIFIEE");
        assertThat(relue.getHistorique()).allSatisfy(e -> {
            assertThat(e.getAuteur().getId()).isEqualTo(rt.getId());
            assertThat(e.getDemande().getId()).isEqualTo(relue.getId());
            assertThat(e.getDateEvenement()).isNotNull();
        });
    }
}
