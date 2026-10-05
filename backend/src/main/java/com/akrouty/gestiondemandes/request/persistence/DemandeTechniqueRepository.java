package com.akrouty.gestiondemandes.request.persistence;

import com.akrouty.gestiondemandes.request.domain.DemandeTechnique;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository de {@link DemandeTechnique} : CRUD technique JPA, recherche par
 * référence métier et test d'existence par référence.
 *
 * <p>Aucun repository autonome {@code HistoriqueDemandeRepository} :
 * l'historique est manipulé dans le contexte de la demande (ADR-003).
 * Les requêtes de listing/filtres viendront avec l'API métier.</p>
 */
public interface DemandeTechniqueRepository extends JpaRepository<DemandeTechnique, Long> {

	Optional<DemandeTechnique> findByReference(String reference);

	boolean existsByReference(String reference);
}
