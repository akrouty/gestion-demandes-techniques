package com.akrouty.gestiondemandes.request.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.akrouty.gestiondemandes.request.domain.Client;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests de persistance de {@link Client} sur le H2 de test isolé.
 */
@SpringBootTest
@Transactional
class ClientRepositoryTest {

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void client_est_persiste_et_relu() {
		Client sauvegarde = clientRepository.save(
				new Client("Client Test", "contact@example.com", "+216 71 123 456"));
		Long id = sauvegarde.getId();
		entityManager.flush();
		entityManager.clear();

		Client relu = clientRepository.findById(id).orElseThrow();
		assertThat(relu.getNom()).isEqualTo("Client Test");
		assertThat(relu.getEmail()).isEqualTo("contact@example.com");
		assertThat(relu.getTelephone()).isEqualTo("+216 71 123 456");
	}

	@Test
	void deux_clients_peuvent_avoir_le_meme_email() {
		Client premier = clientRepository.save(
				new Client("Client 1", "partage@example.com", "+216 71 000 001"));
		Client second = clientRepository.save(
				new Client("Client 2", "partage@example.com", "+216 71 000 002"));
		entityManager.flush();
		entityManager.clear();

		assertThat(clientRepository.findById(premier.getId())).isPresent();
		assertThat(clientRepository.findById(second.getId())).isPresent();
	}
}
