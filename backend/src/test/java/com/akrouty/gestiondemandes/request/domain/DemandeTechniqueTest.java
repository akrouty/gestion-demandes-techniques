package com.akrouty.gestiondemandes.request.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.akrouty.gestiondemandes.identity.domain.Role;
import com.akrouty.gestiondemandes.identity.domain.Utilisateur;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests unitaires de domaine de {@link DemandeTechnique} (Bloc 3 §37) :
 * création, transitions du cycle de vie, terminalité, dates et historisation.
 * Aucun Spring : le domaine ne dépend d'aucun framework.
 */
class DemandeTechniqueTest {

	private Client client;
	private Utilisateur createur;
	private Utilisateur agentA;
	private Utilisateur agentB;
	private Utilisateur rt;
	private Instant maintenant;

	@BeforeEach
	void preparer() {
		maintenant = Instant.now().truncatedTo(ChronoUnit.MICROS);
		client = new Client("Client Test", "client@example.com", "+216 71 123 456");
		createur = utilisateur(1L, "rt@example.com", Role.RESPONSABLE_TECHNIQUE);
		agentA = utilisateur(2L, "agent-a@example.com", Role.AGENT_TECHNIQUE);
		agentB = utilisateur(3L, "agent-b@example.com", Role.AGENT_TECHNIQUE);
		rt = utilisateur(4L, "rt2@example.com", Role.RESPONSABLE_TECHNIQUE);
	}

	// ---------------------------------------------------------------- création

	@Test
	void creation_impose_nouvelle_et_dates_serveur() {
		DemandeTechnique demande = nouvelleDemande("REF-1");

		assertThat(demande.getStatut()).isEqualTo(StatutDemande.NOUVELLE);
		assertThat(demande.getReference()).isEqualTo("REF-1");
		assertThat(demande.getDateCreation()).isEqualTo(maintenant);
		assertThat(demande.getDateModification()).isEqualTo(maintenant);
		assertThat(demande.getDateResolution()).isNull();
		assertThat(demande.getDateCloture()).isNull();
		assertThat(demande.getDateAnnulation()).isNull();
		assertThat(demande.getAgentAffecte()).isNull();
		assertThat(demande.getDescriptionTraitement()).isNull();
		assertThat(demande.getSolution()).isNull();
		assertThat(demande.getMotifAnnulation()).isNull();
	}

	@Test
	void creation_sans_constructeur_public_etat_arbitraire() {
		// Aucun constructeur public fonctionnel : seul {@link #creer} existe.
		assertThat(DemandeTechnique.class.getConstructors()).isEmpty();
	}
	// ---------------------------------------------------------------- affectation

	@Test
	void affectation_nouvelle_vers_assignee_avec_evenement() {
		DemandeTechnique demande = nouvelleDemande("REF-2");

		demande.affecter(agentA, rt, maintenant.plusSeconds(1));

		assertThat(demande.getStatut()).isEqualTo(StatutDemande.ASSIGNEE);
		assertThat(demande.getAgentAffecte()).isSameAs(agentA);
		assertThat(demande.getHistorique()).hasSize(1);
		HistoriqueDemande evenement = demande.getHistorique().getFirst();
		assertThat(evenement.getTypeEvenement()).isEqualTo(EvenementsDemande.AFFECTATION);
		assertThat(evenement.getAncienneValeur()).isNull();
		assertThat(evenement.getNouvelleValeur()).isEqualTo("agent-a@example.com");
		assertThat(evenement.getAuteur()).isSameAs(rt);
	}

	@Test
	void reaffectation_assignee_reste_assignee() {
		DemandeTechnique demande = nouvelleDemande("REF-3");
		demande.affecter(agentA, rt, maintenant);

		demande.affecter(agentB, rt, maintenant.plusSeconds(1));

		assertThat(demande.getStatut()).isEqualTo(StatutDemande.ASSIGNEE);
		assertThat(demande.getAgentAffecte()).isSameAs(agentB);
		assertThat(demande.getHistorique()).hasSize(2);
		HistoriqueDemande evenement = demande.getHistorique().get(1);
		assertThat(evenement.getTypeEvenement()).isEqualTo(EvenementsDemande.REAFFECTATION);
		assertThat(evenement.getAncienneValeur()).isEqualTo("agent-a@example.com");
		assertThat(evenement.getNouvelleValeur()).isEqualTo("agent-b@example.com");
	}

	@Test
	void reaffectation_en_cours_vers_autre_agent_retourne_a_assignee() {
		DemandeTechnique demande = demandeEnCours("REF-4");
		int evenementsAvant = demande.getHistorique().size();

		demande.affecter(agentB, rt, maintenant.plusSeconds(10));

		// RM34 : EN_COURS réaffecté à un AUTRE Agent → ASSIGNEE.
		assertThat(demande.getStatut()).isEqualTo(StatutDemande.ASSIGNEE);
		assertThat(demande.getAgentAffecte()).isSameAs(agentB);
		assertThat(demande.getHistorique()).hasSize(evenementsAvant + 1);
		assertThat(demande.getHistorique().getLast().getTypeEvenement())
				.isEqualTo(EvenementsDemande.REAFFECTATION);
		// Le traitement existant n'est jamais supprimé par une réaffectation.
		assertThat(demande.getDescriptionTraitement()).isEqualTo("Diagnostic en cours");
	}

	@Test
	void meme_agent_sur_en_cours_ne_provoque_pas_de_fausse_reaffectation() {
		DemandeTechnique demande = demandeEnCours("REF-5");
		int evenementsAvant = demande.getHistorique().size();

		demande.affecter(agentA, rt, maintenant.plusSeconds(10));

		// Même Agent : aucun changement de statut, aucun événement (RM34).
		assertThat(demande.getStatut()).isEqualTo(StatutDemande.EN_COURS);
		assertThat(demande.getAgentAffecte()).isSameAs(agentA);
		assertThat(demande.getHistorique()).hasSize(evenementsAvant);
	}

	@Test
	void affectation_depuis_resolue_est_refusee() {
		DemandeTechnique demande = demandeResolue("REF-6");

		assertThatThrownBy(() -> demande.affecter(agentB, rt, maintenant))
				.isInstanceOf(TransitionInvalidException.class);
		assertThat(demande.getStatut()).isEqualTo(StatutDemande.RESOLUE);
	}
	// ---------------------------------------------------------------- démarrage & résolution

	@Test
	void demarrage_assignee_vers_en_cours_avec_evenement() {
		DemandeTechnique demande = nouvelleDemande("REF-7");
		demande.affecter(agentA, rt, maintenant);

		demande.demarrerTraitement(agentA, maintenant.plusSeconds(1));

		assertThat(demande.getStatut()).isEqualTo(StatutDemande.EN_COURS);
		assertThat(demande.getHistorique().getLast().getTypeEvenement())
				.isEqualTo(EvenementsDemande.TRAITEMENT_DEMARRE);
	}

	@Test
	void demarrage_depuis_nouvelle_est_refuse() {
		DemandeTechnique demande = nouvelleDemande("REF-8");

		assertThatThrownBy(() -> demande.demarrerTraitement(agentA, maintenant))
				.isInstanceOf(TransitionInvalidException.class);
		assertThat(demande.getStatut()).isEqualTo(StatutDemande.NOUVELLE);
	}

	@Test
	void resolution_en_cours_avec_solution_vers_resolue() {
		DemandeTechnique demande = demandeEnCoursAvecSolution("REF-9");

		demande.resoudre(agentA, maintenant.plusSeconds(10));

		assertThat(demande.getStatut()).isEqualTo(StatutDemande.RESOLUE);
		assertThat(demande.getDateResolution()).isEqualTo(maintenant.plusSeconds(10));
		assertThat(demande.getDateModification()).isEqualTo(maintenant.plusSeconds(10));
		assertThat(demande.getHistorique().getLast().getTypeEvenement())
				.isEqualTo(EvenementsDemande.RESOLUTION);
	}

	@Test
	void resolution_sans_solution_est_refusee_sans_modification() {
		DemandeTechnique demande = demandeEnCours("REF-10");
		int evenementsAvant = demande.getHistorique().size();

		assertThatThrownBy(() -> demande.resoudre(agentA, maintenant))
				.isInstanceOf(SolutionRequiseException.class);

		// Échec : ni changement partiel, ni événement parasite.
		assertThat(demande.getStatut()).isEqualTo(StatutDemande.EN_COURS);
		assertThat(demande.getDateResolution()).isNull();
		assertThat(demande.getHistorique()).hasSize(evenementsAvant);
	}

	@Test
	void resolution_avec_solution_blanche_est_refusee() {
		DemandeTechnique demande = demandeEnCours("REF-11");
		demande.majTraitement(null, false, "   ", true, agentA, maintenant);

		assertThatThrownBy(() -> demande.resoudre(agentA, maintenant))
				.isInstanceOf(SolutionRequiseException.class);
	}

	@Test
	void maj_traitement_historise_uniquement_les_changements_reels() {
		DemandeTechnique demande = demandeEnCours("REF-12");
		int avant = demande.getHistorique().size();

		// Valeur identique : aucun événement.
		demande.majTraitement("Diagnostic en cours", true, null, false, agentA, maintenant);
		assertThat(demande.getHistorique()).hasSize(avant);

		// Nouvelle solution : un seul événement SOLUTION_MODIFIEE.
		demande.majTraitement(null, false, "Correctif appliqué", true, agentA, maintenant);
		assertThat(demande.getHistorique()).hasSize(avant + 1);
		assertThat(demande.getHistorique().getLast().getTypeEvenement())
				.isEqualTo(EvenementsDemande.SOLUTION_MODIFIEE);
		assertThat(demande.getSolution()).isEqualTo("Correctif appliqué");
	}
	// ---------------------------------------------------------------- refus, clôture, annulation, terminalité

	@Test
	void refus_resolution_resolue_vers_en_cours_conserve_les_donnees() {
		DemandeTechnique demande = demandeResolue("REF-13");

		demande.refuserResolution(rt, maintenant.plusSeconds(10));

		assertThat(demande.getStatut()).isEqualTo(StatutDemande.EN_COURS);
		assertThat(demande.getDateResolution()).isNull();
		assertThat(demande.getSolution()).isEqualTo("Correctif appliqué");
		assertThat(demande.getDescriptionTraitement()).isEqualTo("Diagnostic en cours");
		assertThat(demande.getHistorique().getLast().getTypeEvenement())
				.isEqualTo(EvenementsDemande.REFUS_RESOLUTION);
		// L'événement RESOLUTION antérieur conserve la traçabilité.
		assertThat(demande.getHistorique())
				.extracting(HistoriqueDemande::getTypeEvenement)
				.contains(EvenementsDemande.RESOLUTION, EvenementsDemande.REFUS_RESOLUTION);
	}

	@Test
	void cloture_resolue_vers_cloturee_avec_date() {
		DemandeTechnique demande = demandeResolue("REF-14");

		demande.cloturer(rt, maintenant.plusSeconds(10));

		assertThat(demande.getStatut()).isEqualTo(StatutDemande.CLOTUREE);
		assertThat(demande.getDateCloture()).isEqualTo(maintenant.plusSeconds(10));
		assertThat(demande.getHistorique().getLast().getTypeEvenement())
				.isEqualTo(EvenementsDemande.CLOTURE);
	}

	@Test
	void cloture_et_refus_depuis_etat_incompatible_sont_refuses() {
		DemandeTechnique demande = demandeEnCours("REF-15");

		assertThatThrownBy(() -> demande.cloturer(rt, maintenant))
				.isInstanceOf(TransitionInvalidException.class);
		assertThatThrownBy(() -> demande.refuserResolution(rt, maintenant))
				.isInstanceOf(TransitionInvalidException.class);
	}

	@Test
	void annulation_depuis_etats_actifs_autorisee_avec_motif_et_dates() {
		DemandeTechnique demande = nouvelleDemande("REF-16");

		demande.annuler("Client parti ailleurs", rt, maintenant.plusSeconds(1));

		assertThat(demande.getStatut()).isEqualTo(StatutDemande.ANNULEE);
		assertThat(demande.getMotifAnnulation()).isEqualTo("Client parti ailleurs");
		assertThat(demande.getDateAnnulation()).isEqualTo(maintenant.plusSeconds(1));
		assertThat(demande.getDateModification()).isEqualTo(maintenant.plusSeconds(1));
		assertThat(demande.getHistorique().getLast().getTypeEvenement())
				.isEqualTo(EvenementsDemande.ANNULATION);
	}

	@Test
	void annulation_depuis_resolue_est_refusee() {
		DemandeTechnique demande = demandeResolue("REF-17");

		assertThatThrownBy(() -> demande.annuler("motif", rt, maintenant))
				.isInstanceOf(TransitionInvalidException.class);
		assertThat(demande.getStatut()).isEqualTo(StatutDemande.RESOLUE);
	}

	@Test
	void annulation_depuis_etats_terminaux_est_refusee() {
		DemandeTechnique cloturee = demandeResolue("REF-18");
		cloturee.cloturer(rt, maintenant);

		assertThatThrownBy(() -> cloturee.annuler("motif", rt, maintenant))
				.isInstanceOf(DemandeTermineeException.class);

		DemandeTechnique annulee = nouvelleDemande("REF-19");
		annulee.annuler("motif", rt, maintenant);

		assertThatThrownBy(() -> annulee.annuler("motif bis", rt, maintenant.plusSeconds(1)))
				.isInstanceOf(DemandeTermineeException.class);
	}

	@Test
	void annulation_sans_motif_est_refusee() {
		DemandeTechnique demande = nouvelleDemande("REF-20");

		assertThatThrownBy(() -> demande.annuler("   ", rt, maintenant))
				.isInstanceOf(IllegalArgumentException.class);
		assertThat(demande.getStatut()).isEqualTo(StatutDemande.NOUVELLE);
	}

	@Test
	void demande_terminee_n_est_plus_modifiable_fonctionnellement() {
		DemandeTechnique cloturee = demandeResolue("REF-21");
		cloturee.cloturer(rt, maintenant);

		assertThatThrownBy(() -> cloturee.qualifier(Categorie.AUTRE, Priorite.BASSE, rt, maintenant))
				.isInstanceOf(DemandeTermineeException.class);
		assertThatThrownBy(() -> cloturee.affecter(agentB, rt, maintenant))
				.isInstanceOf(DemandeTermineeException.class);
		assertThatThrownBy(() -> cloturee.demarrerTraitement(agentA, maintenant))
				.isInstanceOf(DemandeTermineeException.class);
		assertThatThrownBy(() -> cloturee.resoudre(agentA, maintenant))
				.isInstanceOf(DemandeTermineeException.class);
		assertThat(cloturee.getStatut()).isEqualTo(StatutDemande.CLOTUREE);
	}
	// ---------------------------------------------------------------- qualification & utilitaires

	@Test
	void qualification_avec_valeurs_identiques_ne_genere_aucun_evenement() {
		DemandeTechnique demande = nouvelleDemande("REF-22");
		int evenementsAvant = demande.getHistorique().size();

		demande.qualifier(Categorie.PROTECTION_INCENDIE, Priorite.HAUTE, rt, maintenant.plusSeconds(1));

		assertThat(demande.getHistorique()).hasSize(evenementsAvant);
		assertThat(demande.getDateModification()).isEqualTo(maintenant);
	}

	@Test
	void qualification_historise_chaque_modification_reelle() {
		DemandeTechnique demande = nouvelleDemande("REF-23");

		demande.qualifier(Categorie.NOTE_CALCUL, Priorite.CRITIQUE, rt, maintenant.plusSeconds(1));

		assertThat(demande.getCategorie()).isEqualTo(Categorie.NOTE_CALCUL);
		assertThat(demande.getPriorite()).isEqualTo(Priorite.CRITIQUE);
		// L'événement CREATION est ajouté par le service applicatif, pas par la factory.
		assertThat(demande.getHistorique())
				.extracting(HistoriqueDemande::getTypeEvenement)
				.containsExactly(EvenementsDemande.CATEGORIE_MODIFIEE,
						EvenementsDemande.PRIORITE_MODIFIEE);
		assertThat(demande.getHistorique().getFirst().getAncienneValeur())
				.isEqualTo("PROTECTION_INCENDIE");
		assertThat(demande.getHistorique().getFirst().getNouvelleValeur()).isEqualTo("NOTE_CALCUL");
	}

	@Test
	void qualification_partielle_historise_uniquement_le_changement_reel() {
		DemandeTechnique demande = nouvelleDemande("REF-24");

		// Même catégorie, priorité différente : un seul événement de qualification.
		demande.qualifier(Categorie.PROTECTION_INCENDIE, Priorite.BASSE, rt, maintenant.plusSeconds(1));

		assertThat(demande.getHistorique())
				.extracting(HistoriqueDemande::getTypeEvenement)
				.containsExactly(EvenementsDemande.PRIORITE_MODIFIEE);
	}

	// ---------------------------------------------------------------- utilitaires

	private DemandeTechnique nouvelleDemande(String reference) {
		return DemandeTechnique.creer(
				reference, "Titre", "Description", Categorie.PROTECTION_INCENDIE,
				Priorite.HAUTE, client, createur, maintenant);
	}

	private DemandeTechnique demandeEnCours(String reference) {
		DemandeTechnique demande = nouvelleDemande(reference);
		demande.ajouterEvenement(maintenant, EvenementsDemande.CREATION, null, "NOUVELLE", createur);
		demande.affecter(agentA, rt, maintenant);
		demande.demarrerTraitement(agentA, maintenant);
		demande.majTraitement("Diagnostic en cours", true, null, false, agentA, maintenant);
		return demande;
	}

	private DemandeTechnique demandeEnCoursAvecSolution(String reference) {
		DemandeTechnique demande = demandeEnCours(reference);
		demande.majTraitement(null, false, "Correctif appliqué", true, agentA, maintenant);
		return demande;
	}

	private DemandeTechnique demandeResolue(String reference) {
		DemandeTechnique demande = demandeEnCoursAvecSolution(reference);
		demande.resoudre(agentA, maintenant);
		return demande;
	}

	/** Utilisateur avec id technique assigné (simulation de persistance). */
	private static Utilisateur utilisateur(long id, String email, Role... roles) {
		try {
			Utilisateur utilisateur = new Utilisateur("Utilisateur " + id, email, true, "hash", Set.of(roles));
			Field champ = Utilisateur.class.getDeclaredField("id");
			champ.setAccessible(true);
			champ.set(utilisateur, id);
			return utilisateur;
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Assignation de l'id de test impossible", e);
		}
	}
}