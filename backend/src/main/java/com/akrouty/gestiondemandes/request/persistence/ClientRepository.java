package com.akrouty.gestiondemandes.request.persistence;

import com.akrouty.gestiondemandes.request.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository technique du module {@code request} pour {@link Client}.
 */
public interface ClientRepository extends JpaRepository<Client, Long> {
}
