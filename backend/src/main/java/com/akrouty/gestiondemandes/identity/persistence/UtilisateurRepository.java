package com.akrouty.gestiondemandes.identity.persistence;

import com.akrouty.gestiondemandes.identity.domain.Utilisateur;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository du module {@code identity} : CRUD technique JPA, recherche par
 * email et test d'existence par email. L'email doit être normalisé par
 * l'appelant conformément à ADR-003.
 */
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

	Optional<Utilisateur> findByEmail(String email);

	boolean existsByEmail(String email);
}
