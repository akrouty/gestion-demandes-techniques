package com.akrouty.gestiondemandes.identity.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.akrouty.gestiondemandes.identity.domain.Role;
import com.akrouty.gestiondemandes.identity.domain.Utilisateur;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests de persistance de {@link Utilisateur} sur le H2 de test isolé.
 */
@SpringBootTest
@Transactional
class UtilisateurRepositoryTest {

	/**
	 * Chaîne de forme hash (donnée de test uniquement) : le hachage réel est
	 * implémenté dans le bloc suivant, jamais de mot de passe en clair ici.
	 */
	private static final String HASH_DE_TEST = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

	@Autowired
	private UtilisateurRepository utilisateurRepository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Utilisateur nouvelUtilisateur(String email, Role... roles) {
		return new Utilisateur("Alice Martin", email, true, HASH_DE_TEST, Set.of(roles));
	}

	@Test
	void utilisateur_est_persiste_et_relu_avec_roles_multiples() {
		Utilisateur sauvegarde = utilisateurRepository.save(
				nouvelUtilisateur("alice@example.com", Role.RESPONSABLE_TECHNIQUE, Role.ADMINISTRATEUR));
		Long id = sauvegarde.getId();
		entityManager.flush();
		entityManager.clear();

		Utilisateur relu = utilisateurRepository.findById(id).orElseThrow();
		assertThat(relu.getNom()).isEqualTo("Alice Martin");
		assertThat(relu.getEmail()).isEqualTo("alice@example.com");
		assertThat(relu.isActif()).isTrue();
		assertThat(relu.getPasswordHash()).isEqualTo(HASH_DE_TEST);
		assertThat(relu.getRoles())
				.containsExactlyInAnyOrder(Role.RESPONSABLE_TECHNIQUE, Role.ADMINISTRATEUR);
	}

	@Test
	void roles_sont_persistes_sous_forme_textuelle() {
		Utilisateur sauvegarde = nouvelUtilisateur("bob@example.com", Role.AGENT_TECHNIQUE);
		utilisateurRepository.save(sauvegarde);
		entityManager.flush();

		List<String> rolesEnBase = jdbcTemplate.queryForList(
				"SELECT role FROM utilisateur_role WHERE utilisateur_id = ?",
				String.class,
				sauvegarde.getId());
		assertThat(rolesEnBase).containsExactly("AGENT_TECHNIQUE");
	}

	@Test
	void email_est_normalise_avant_persistance_et_recherche_par_email_fonctionne() {
		Utilisateur sauvegarde = utilisateurRepository.save(
				nouvelUtilisateur("  Charlie@Example.COM  "));
		Long id = sauvegarde.getId();
		assertThat(sauvegarde.getEmail()).isEqualTo("charlie@example.com");
		entityManager.flush();
		entityManager.clear();

		Utilisateur relu = utilisateurRepository.findById(id).orElseThrow();
		assertThat(relu.getEmail()).isEqualTo("charlie@example.com");

		assertThat(utilisateurRepository.existsByEmail("charlie@example.com")).isTrue();
		assertThat(utilisateurRepository.findByEmail("charlie@example.com")).isPresent();
		assertThat(utilisateurRepository.findByEmail("charlie@example.com").orElseThrow().getId())
				.isEqualTo(id);
	}

	@Test
	void email_utilisateur_duplique_est_refuse() {
		utilisateurRepository.saveAndFlush(nouvelUtilisateur("duplique@example.com"));

		Utilisateur doublon = nouvelUtilisateur("duplique@example.com");
		assertThatThrownBy(() -> utilisateurRepository.saveAndFlush(doublon))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}
