package com.akrouty.gestiondemandes.identity.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.akrouty.gestiondemandes.identity.domain.Role;
import com.akrouty.gestiondemandes.identity.domain.Utilisateur;
import com.akrouty.gestiondemandes.support.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Tests d'intégration de l'administration des utilisateurs (API-CONTRACT-V1)
 * : création, consultation, modification, activation, désactivation, rôles
 * métier, email, mot de passe et codes d'erreur.
 */
class UtilisateurApiTest extends ApiTestSupport {

	private String jetonAdmin;

	@BeforeEach
	void creerAdministrateur() {
		Utilisateur admin = creerUtilisateur("Admin Principal", "admin@example.com", true, Role.ADMINISTRATEUR);
		jetonAdmin = jeton(admin);
	}

	private String administration() {
		return BEARER_PREFIX + jetonAdmin;
	}

	// ---------------------------------------------------------------- création

	@Test
	void creation_retourne_201_le_detail_et_le_location() throws Exception {
		mockMvc.perform(post("/api/v1/utilisateurs")
						.header("Authorization", administration())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nom":"Jean Martin","email":"Jean@Example.COM","actif":true,
								 "rolesMetier":["RESPONSABLE_TECHNIQUE"],"password":"MotDePasseDeTest"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", matchesPattern("/api/v1/utilisateurs/\\d+")))
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.nom").value("Jean Martin"))
				.andExpect(jsonPath("$.email").value("jean@example.com"))
				.andExpect(jsonPath("$.actif").value(true))
				.andExpect(jsonPath("$.roles[0]").value("RESPONSABLE_TECHNIQUE"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void creation_persiste_un_hash_bcrypt_et_jamais_le_mot_de_passe_en_clair() throws Exception {
		mockMvc.perform(post("/api/v1/utilisateurs")
						.header("Authorization", administration())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nom":"Alice","email":"alice@example.com","actif":true,
								 "rolesMetier":["AGENT_TECHNIQUE"],"password":"MotDePasseDeTest"}
								"""))
				.andExpect(status().isCreated());

		Utilisateur enBase = utilisateurRepository.findByEmail("alice@example.com").orElseThrow();
		assertThat(enBase.getPasswordHash()).isNotEqualTo("MotDePasseDeTest");
		assertThat(enBase.getPasswordHash()).startsWith("$2");
		assertThat(passwordEncoder.matches("MotDePasseDeTest", enBase.getPasswordHash())).isTrue();
		assertThat(enBase.getPasswordHash()).doesNotContain("MotDePasseDeTest");
	}

	@Test
	void creation_avec_email_duplique_retourne_409() throws Exception {
		creerUtilisateur("Existant", "deja@example.com", true, Role.AGENT_TECHNIQUE);

		mockMvc.perform(post("/api/v1/utilisateurs")
						.header("Authorization", administration())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nom":"Doublon","email":"DEJA@example.com","actif":true,
								 "rolesMetier":[],"password":"MotDePasseDeTest"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_DEJA_UTILISE"));
	}

	@Test
	void creation_avec_password_trop_court_retourne_400() throws Exception {
		mockMvc.perform(post("/api/v1/utilisateurs")
						.header("Authorization", administration())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nom":"Court","email":"court@example.com","actif":true,
								 "rolesMetier":[],"password":"court"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MOT_DE_PASSE_INVALIDE"));

		assertThat(utilisateurRepository.existsByEmail("court@example.com")).isFalse();
	}

	@Test
	void creation_avec_role_administrateur_dans_rolesMetier_retourne_400() throws Exception {
		mockMvc.perform(post("/api/v1/utilisateurs")
						.header("Authorization", administration())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nom":"Imposteur","email":"imposteur@example.com","actif":true,
								 "rolesMetier":["ADMINISTRATEUR"],"password":"MotDePasseDeTest"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("ROLE_METIER_INVALIDE"));

		assertThat(utilisateurRepository.existsByEmail("imposteur@example.com")).isFalse();
	}

	@Test
	void creation_avec_roles_metier_vides_est_autorisee() throws Exception {
		mockMvc.perform(post("/api/v1/utilisateurs")
						.header("Authorization", administration())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nom":"Sans Role","email":"sansrole@example.com","actif":true,
								 "rolesMetier":[],"password":"MotDePasseDeTest"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.roles").isEmpty());
	}

	@Test
	void creation_avec_champs_invalides_retourne_400_avec_field_errors() throws Exception {
		mockMvc.perform(post("/api/v1/utilisateurs")
						.header("Authorization", administration())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nom":" ","email":"invalide","rolesMetier":[],"password":"MotDePasseDeTest"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION"))
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'nom')].code").value(hasItem("OBLIGATOIRE")))
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')].code").value(hasItem("EMAIL_INVALIDE")))
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'actif')].code").value(hasItem("OBLIGATOIRE")));
	}

	// ---------------------------------------------------------------- lectures

	@Test
	void get_utilisateur_inconnu_retourne_404() throws Exception {
		mockMvc.perform(get("/api/v1/utilisateurs/999999")
						.header("Authorization", administration()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("UTILISATEUR_INTROUVABLE"));
	}

	@Test
	void liste_est_paginee_au_format_du_contrat() throws Exception {
		creerUtilisateur("Un", "u1@example.com", true, Role.AGENT_TECHNIQUE);
		creerUtilisateur("Deux", "u2@example.com", true, Role.AGENT_TECHNIQUE);
		creerUtilisateur("Trois", "u3@example.com", true, Role.RESPONSABLE_TECHNIQUE);

		mockMvc.perform(get("/api/v1/utilisateurs?page=0&size=2")
						.header("Authorization", administration()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(2))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(2))
				.andExpect(jsonPath("$.totalElements").value(4))
				.andExpect(jsonPath("$.totalPages").value(2));

		mockMvc.perform(get("/api/v1/utilisateurs?page=1&size=3")
						.header("Authorization", administration()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.totalElements").value(4));
	}

	@Test
	void liste_avec_tri_ou_pagination_invalides_retourne_400() throws Exception {
		mockMvc.perform(get("/api/v1/utilisateurs?sort=inconnu")
						.header("Authorization", administration()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("PARAMETRE_INVALIDE"));

		mockMvc.perform(get("/api/v1/utilisateurs?page=-1")
						.header("Authorization", administration()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("PARAMETRE_INVALIDE"));
	}

	@Test
	void get_detail_retourne_l_utilisateur_sans_credential() throws Exception {
		Utilisateur cible = creerUtilisateur("Cible", "cible@example.com", true, Role.AGENT_TECHNIQUE);

		mockMvc.perform(get("/api/v1/utilisateurs/" + cible.getId())
						.header("Authorization", administration()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(cible.getId().intValue()))
				.andExpect(jsonPath("$.email").value("cible@example.com"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	// ---------------------------------------------------------------- modification

	@Test
	void modification_change_nom_et_email_sans_toucher_actif_roles_ni_password_hash() throws Exception {
		Utilisateur cible = creerUtilisateur("Avant", "avant@example.com", true, Role.RESPONSABLE_TECHNIQUE);
		String hashAvant = cible.getPasswordHash();

		mockMvc.perform(put("/api/v1/utilisateurs/" + cible.getId())
						.header("Authorization", administration())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nom":"Apres","email":"Apres@Example.NET"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nom").value("Apres"))
				.andExpect(jsonPath("$.email").value("apres@example.net"))
				.andExpect(jsonPath("$.actif").value(true))
				.andExpect(jsonPath("$.roles[0]").value("RESPONSABLE_TECHNIQUE"));

		Utilisateur relu = rechargerAvecRoles(cible.getId());
		assertThat(relu.isActif()).isTrue();
		assertThat(relu.getRoles()).containsExactly(Role.RESPONSABLE_TECHNIQUE);
		assertThat(relu.getPasswordHash()).isEqualTo(hashAvant);
	}

	@Test
	void modification_avec_email_deja_utilise_retourne_409() throws Exception {
		creerUtilisateur("Autre", "autre@example.com", true, Role.AGENT_TECHNIQUE);
		Utilisateur cible = creerUtilisateur("Cible", "cible@example.com", true, Role.AGENT_TECHNIQUE);

		mockMvc.perform(put("/api/v1/utilisateurs/" + cible.getId())
						.header("Authorization", administration())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nom":"Cible","email":"AUTRE@example.com"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_DEJA_UTILISE"));
	}

	// ---------------------------------------------------------------- activation

	@Test
	void activation_et_desactivation_changent_l_etat_actif() throws Exception {
		Utilisateur cible = creerUtilisateur("Inactif", "inactif@example.com", false, Role.AGENT_TECHNIQUE);

		mockMvc.perform(post("/api/v1/utilisateurs/" + cible.getId() + "/activation")
						.header("Authorization", administration()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.actif").value(true));
		assertThat(utilisateurRepository.findById(cible.getId()).orElseThrow().isActif()).isTrue();

		mockMvc.perform(post("/api/v1/utilisateurs/" + cible.getId() + "/desactivation")
						.header("Authorization", administration()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.actif").value(false));
		assertThat(utilisateurRepository.findById(cible.getId()).orElseThrow().isActif()).isFalse();
	}

	// ---------------------------------------------------------------- rôles métier

	@Test
	void rolesMetier_remplace_rt_at_conserve_administrateur_existant() throws Exception {
		Utilisateur cible = creerUtilisateur(
				"Multi", "multi@example.com", true, Role.RESPONSABLE_TECHNIQUE, Role.ADMINISTRATEUR);

		mockMvc.perform(put("/api/v1/utilisateurs/" + cible.getId() + "/roles-metier")
						.header("Authorization", administration())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"rolesMetier":["AGENT_TECHNIQUE"]}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.roles", containsInAnyOrder("ADMINISTRATEUR", "AGENT_TECHNIQUE")));

		assertThat(rechargerAvecRoles(cible.getId()).getRoles())
				.containsExactlyInAnyOrder(Role.ADMINISTRATEUR, Role.AGENT_TECHNIQUE);
	}

	@Test
	void rolesMetier_avec_administrateur_retourne_400() throws Exception {
		Utilisateur cible = creerUtilisateur("Cible", "cible@example.com", true, Role.AGENT_TECHNIQUE);

		mockMvc.perform(put("/api/v1/utilisateurs/" + cible.getId() + "/roles-metier")
						.header("Authorization", administration())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"rolesMetier":["ADMINISTRATEUR"]}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("ROLE_METIER_INVALIDE"));

		assertThat(rechargerAvecRoles(cible.getId()).getRoles())
				.containsExactly(Role.AGENT_TECHNIQUE);
	}

	@Test
	void rolesMetier_remplacement_complet_et_ensemble_vide_autorise() throws Exception {
		Utilisateur cible = creerUtilisateur(
				"Double", "double@example.com", true, Role.RESPONSABLE_TECHNIQUE, Role.AGENT_TECHNIQUE);

		mockMvc.perform(put("/api/v1/utilisateurs/" + cible.getId() + "/roles-metier")
						.header("Authorization", administration())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"rolesMetier":["AGENT_TECHNIQUE"]}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.roles[0]").value("AGENT_TECHNIQUE"));

		mockMvc.perform(put("/api/v1/utilisateurs/" + cible.getId() + "/roles-metier")
						.header("Authorization", administration())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"rolesMetier":[]}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.roles").isEmpty());
	}
}
