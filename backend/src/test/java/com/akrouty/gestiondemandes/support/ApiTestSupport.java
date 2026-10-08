package com.akrouty.gestiondemandes.support;

import com.akrouty.gestiondemandes.identity.domain.Role;
import com.akrouty.gestiondemandes.identity.domain.Utilisateur;
import com.akrouty.gestiondemandes.identity.persistence.UtilisateurRepository;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base des tests d'API du Bloc 2 (H2 de test isolé + paramètres de sécurité
 * TEST de {@code src/test/resources}).
 *
 * <p>Les fixtures créent directement les utilisateurs (dont l'administrateur)
 * : aucun bootstrap de premier ADMINISTRATEUR n'existe dans les décisions
 * validées.</p>
 *
 * <p>Bloc 3 : le nettoyage supprime d'abord historique, demandes et clients
 * (clés étrangères) puis les utilisateurs.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class ApiTestSupport {

	/** Mot de passe de test uniquement (respecte PASSWORD_MIN_LENGTH de test : 8). */
	protected static final String MOT_DE_PASSE_DE_TEST = "MotDePasseDeTest";

	/** Secret HMAC TEST — identique à {@code src/test/resources/application.properties} (64 octets). */
	protected static final String SECRET_JWT_DE_TEST =
			"test-only-jwt-hmac-signing-key-0123456789abcdef0123456789abcdefz";

	protected static final String ISSUER_DE_TEST = "gestion-demandes-techniques-test";
	protected static final String BEARER_PREFIX = "Bearer ";

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected UtilisateurRepository utilisateurRepository;

	@Autowired
	protected PasswordEncoder passwordEncoder;

	@Autowired
	protected JdbcTemplate jdbcTemplate;

	@BeforeEach
	void nettoyerLesDonnees() {
		// Ordre des clés étrangères : historique → demandes → clients → utilisateurs.
		jdbcTemplate.update("DELETE FROM historique_demande");
		jdbcTemplate.update("DELETE FROM demande_technique");
		jdbcTemplate.update("DELETE FROM client");
		utilisateurRepository.deleteAll();
	}

	protected Utilisateur creerUtilisateur(String nom, String email, boolean actif, Role... roles) {
		return utilisateurRepository.save(new Utilisateur(
				nom,
				email,
				actif,
				passwordEncoder.encode(MOT_DE_PASSE_DE_TEST),
				Set.of(roles)));
	}

	/** Réalise un login et retourne le {@code accessToken} (échec = exception). */
	protected String seConnecter(String email, String motDePasse) {
		try {
			MvcResult resultat = mockMvc.perform(post("/api/v1/auth/login")
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"email\":\"" + email + "\",\"password\":\"" + motDePasse + "\"}"))
					.andExpect(status().isOk())
					.andReturn();
			return JsonPath.read(corps(resultat), "$.accessToken");
		} catch (Exception e) {
			throw new IllegalStateException("Login de test impossible", e);
		}
	}

	/**
	 * Recharge un utilisateur avec ses rôles chargés (join fetch) : indispensable
	 * pour lire {@code getRoles()} hors transaction, la collection étant LAZY.
	 */
	protected Utilisateur rechargerAvecRoles(Long id) {
		return utilisateurRepository.findAllByIdAvecRoles(java.util.List.of(id)).getFirst();
	}

	protected String jeton(Utilisateur utilisateur) {
		return seConnecter(utilisateur.getEmail(), MOT_DE_PASSE_DE_TEST);
	}

	protected String corps(MvcResult resultat) {
		try {
			return resultat.getResponse().getContentAsString(StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new IllegalStateException("Lecture de la réponse impossible", e);
		}
	}
}