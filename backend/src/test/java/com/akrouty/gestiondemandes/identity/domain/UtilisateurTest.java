package com.akrouty.gestiondemandes.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests unitaires des comportements domaine d'ajout du Bloc 2 :
 * collection de rôles non modifiable depuis l'extérieur, opérations contrôlées.
 */
class UtilisateurTest {

	/** Chaîne technique de hachage factice : jamais un mot de passe en clair. */
	private static final String HASH_TECHNIQUE = "$2a$4$techniqueFacticePourDomaineTestUniquement0000000000000000";

	private Utilisateur administratriceAlice() {
		return new Utilisateur(
				"Alice Martin",
				"  Alice@Example.COM  ",
				true,
				HASH_TECHNIQUE,
				Set.of(Role.RESPONSABLE_TECHNIQUE, Role.ADMINISTRATEUR));
	}

	@Test
	void getRoles_retourne_une_vue_non_modifiable() {
		Utilisateur utilisateur = administratriceAlice();

		assertThatThrownBy(() -> utilisateur.getRoles().add(Role.AGENT_TECHNIQUE))
				.isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> utilisateur.getRoles().remove(Role.RESPONSABLE_TECHNIQUE))
				.isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> utilisateur.getRoles().clear())
				.isInstanceOf(UnsupportedOperationException.class);

		// La collection interne n'a subi aucune modification.
		assertThat(utilisateur.getRoles())
				.containsExactlyInAnyOrder(Role.RESPONSABLE_TECHNIQUE, Role.ADMINISTRATEUR);
	}

	@Test
	void email_normalise_a_la_construction_et_a_la_modification() {
		Utilisateur utilisateur = administratriceAlice();
		assertThat(utilisateur.getEmail()).isEqualTo("alice@example.com");

		utilisateur.modifierInformations("Alice Dupont", "  Alice2@Example.ORG  ");
		assertThat(utilisateur.getEmail()).isEqualTo("alice2@example.org");
	}

	@Test
	void modification_des_informations_ne_change_ni_actif_ni_roles_ni_password_hash() {
		Utilisateur utilisateur = administratriceAlice();

		utilisateur.modifierInformations("Alice Dupont", "alice2@example.com");

		assertThat(utilisateur.getNom()).isEqualTo("Alice Dupont");
		assertThat(utilisateur.isActif()).isTrue();
		assertThat(utilisateur.getRoles())
				.containsExactlyInAnyOrder(Role.RESPONSABLE_TECHNIQUE, Role.ADMINISTRATEUR);
		assertThat(utilisateur.getPasswordHash()).isEqualTo(HASH_TECHNIQUE);
	}

	@Test
	void activer_et_desactiver_changent_uniquement_l_etat_actif() {
		Utilisateur utilisateur = administratriceAlice();

		utilisateur.desactiver();
		assertThat(utilisateur.isActif()).isFalse();
		assertThat(utilisateur.getRoles())
				.containsExactlyInAnyOrder(Role.RESPONSABLE_TECHNIQUE, Role.ADMINISTRATEUR);

		utilisateur.activer();
		assertThat(utilisateur.isActif()).isTrue();
	}

	@Test
	void remplacerRolesMetier_remplace_rt_at_conserve_administrateur() {
		Utilisateur utilisateur = administratriceAlice();

		utilisateur.remplacerRolesMetier(Set.of(Role.AGENT_TECHNIQUE));

		assertThat(utilisateur.getRoles())
				.containsExactlyInAnyOrder(Role.ADMINISTRATEUR, Role.AGENT_TECHNIQUE);
	}

	@Test
	void remplacerRolesMetier_refuse_administrateur_sans_modifier_les_roles() {
		Utilisateur utilisateur = administratriceAlice();

		assertThatThrownBy(() -> utilisateur.remplacerRolesMetier(
				Set.of(Role.RESPONSABLE_TECHNIQUE, Role.ADMINISTRATEUR)))
				.isInstanceOf(RoleAdministrateurNonAttribuableException.class);

		assertThat(utilisateur.getRoles())
				.containsExactlyInAnyOrder(Role.RESPONSABLE_TECHNIQUE, Role.ADMINISTRATEUR);
	}

	@Test
	void remplacerRolesMetier_accepte_un_ensemble_vide() {
		Utilisateur sansRoleMetier = new Utilisateur(
				"Bob", "bob@example.com", true, HASH_TECHNIQUE, Set.of(Role.AGENT_TECHNIQUE));
		sansRoleMetier.remplacerRolesMetier(Set.of());
		assertThat(sansRoleMetier.getRoles()).isEmpty();

		Utilisateur adminSeul = administratriceAlice();
		adminSeul.remplacerRolesMetier(Set.of());
		assertThat(adminSeul.getRoles()).containsExactly(Role.ADMINISTRATEUR);
	}
}