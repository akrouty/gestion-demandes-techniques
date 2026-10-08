package com.akrouty.gestiondemandes.request.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.akrouty.gestiondemandes.GestionDemandesApplication;
import com.akrouty.gestiondemandes.identity.domain.Role;
import com.akrouty.gestiondemandes.identity.domain.Utilisateur;
import com.akrouty.gestiondemandes.identity.persistence.UtilisateurRepository;
import com.akrouty.gestiondemandes.request.domain.Categorie;
import com.akrouty.gestiondemandes.request.domain.Client;
import com.akrouty.gestiondemandes.request.domain.Priorite;
import com.akrouty.gestiondemandes.request.persistence.ClientRepository;
import com.akrouty.gestiondemandes.request.persistence.DemandeTechniqueRepository;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Test d'isolation du cas d'utilisation qualification. */
@SpringBootTest
@org.springframework.transaction.annotation.Transactional
class IsolationTest {

	@Autowired
	DemandeService service;
	@Autowired
	UtilisateurRepository utilisateurRepository;
	@Autowired
	ClientRepository clientRepository;
	@Autowired
	DemandeTechniqueRepository demandeRepository;

	@Test
	void qualifier_fonctionne() {
		Utilisateur rt = utilisateurRepository.save(new Utilisateur(
				"RT", "iso@example.com", true, "hash", Set.of(Role.RESPONSABLE_TECHNIQUE)));
		Client client = clientRepository.save(new Client("C", "c@example.com", "123"));
		DemandeConsultation d = service.creer(rt.getId(), new CreationDemandeCommand(
				"Titre", "Description", Categorie.AUTRE, Priorite.BASSE, client.getId(), null));

		DemandeConsultation modifiee = service.qualifier(
				rt.getId(), d.reference(), Categorie.NOTE_CALCUL, Priorite.HAUTE);

		assertThat(modifiee.categorie()).isEqualTo(Categorie.NOTE_CALCUL);

		// Inspection de la collection avant flush.
		var demande = demandeRepository.findByReference(d.reference()).orElseThrow();
		var evenements = demande.getHistorique();
		System.out.println("NB EVENEMENTS = " + evenements.size());
		for (var e : evenements) {
			System.out.println("EVENT type=" + e.getTypeEvenement()
					+ " auteur=" + e.getAuteur()
					+ " demande=" + e.getDemande());
		}
	}
}
