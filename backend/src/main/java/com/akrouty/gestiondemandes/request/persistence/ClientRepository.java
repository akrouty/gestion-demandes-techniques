package com.akrouty.gestiondemandes.request.persistence;

import com.akrouty.gestiondemandes.request.domain.Client;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository technique du module {@code request} pour {@link Client}.
 */
public interface ClientRepository extends JpaRepository<Client, Long> {

	Optional<Client> findById(Long id);

	/**
	 * Recherche simple destinée à la sélection d'un client pendant
	 * l'enregistrement d'une demande : case-insensitive sur {@code nom},
	 * {@code email} et {@code telephone}. Aucun moteur externe (API-CONTRACT-V1 §9.3).
	 */
	@Query("""
			select c from Client c
			where lower(c.nom) like lower(concat('%', :recherche, '%'))
			   or lower(c.email) like lower(concat('%', :recherche, '%'))
			   or lower(c.telephone) like lower(concat('%', :recherche, '%'))
			""")
	Page<Client> rechercher(@Param("recherche") String recherche, Pageable pageable);
}
