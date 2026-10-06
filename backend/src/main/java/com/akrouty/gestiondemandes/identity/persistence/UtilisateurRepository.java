package com.akrouty.gestiondemandes.identity.persistence;

import com.akrouty.gestiondemandes.identity.domain.Utilisateur;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository du module {@code identity} : CRUD technique JPA, recherche par
 * email et test d'existence par email. L'email doit être normalisé par
 * l'appelant conformément à ADR-003.
 *
 * <p>Seules les méthodes réellement nécessaires sont déclarées : unicité de
 * l'email hors utilisateur courant et chargement ciblé des rôles pour une
 * lecture paginée (les rôles restent {@code LAZY} globalement, ADR-003).</p>
 */
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

	Optional<Utilisateur> findByEmail(String email);

	boolean existsByEmail(String email);

	boolean existsByEmailAndIdNot(String email, Long id);

	/**
	 * Chargement ciblé des rôles d'un ensemble restreint d'utilisateurs
	 * (cas d'utilisation : liste paginée), afin d'éviter les lectures N+1
	 * sans rendre la collection {@code EAGER} globale.
	 */
	@Query("select distinct u from Utilisateur u left join fetch u.roles where u.id in :ids")
	List<Utilisateur> findAllByIdAvecRoles(@Param("ids") Collection<Long> ids);
}
