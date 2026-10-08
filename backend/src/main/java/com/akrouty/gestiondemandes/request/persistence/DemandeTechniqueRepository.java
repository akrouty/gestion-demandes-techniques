package com.akrouty.gestiondemandes.request.persistence;

import com.akrouty.gestiondemandes.request.domain.DemandeTechnique;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository de {@link DemandeTechnique} : CRUD technique JPA, recherche par
 * référence métier, test d'existence par référence et composition des filtres
 * de consultation via {@link JpaSpecificationExecutor} (Bloc 3).
 *
 * <p>Aucun repository autonome {@code HistoriqueDemandeRepository} :
 * l'historique est manipulé dans le contexte de la demande (ADR-003).</p>
 *
 * <p>Les chargements sont ciblés, jamais {@code EAGER} global :</p>
 *
 * <ul>
 *   <li>détail : {@code client + createur + agentAffecte} via {@code EntityGraph}
 *       sur {@code findByReference} ;</li>
 *   <li>liste : filtrage paginé sans fetch (count sûr) puis initialisation
 *       groupée de {@code client + agentAffecte} en UNE requête par
 *       {@link #chargerConsultation(Collection)} — pas de N+1 ;</li>
 *   <li>rôles des utilisateurs et historique ne sont jamais chargés ici.</li>
 * </ul>
 */
public interface DemandeTechniqueRepository
		extends JpaRepository<DemandeTechnique, Long>, JpaSpecificationExecutor<DemandeTechnique> {

	@EntityGraph(attributePaths = {"client", "createur", "agentAffecte"})
	Optional<DemandeTechnique> findByReference(String reference);

	boolean existsByReference(String reference);

	/**
	 * Initialisation groupée des associations de consultation (client + agent
	 * affecté) d'un ensemble restreint de demandes déjà paginées. Les instances
	 * retournées sont celles de la session de persistance : les proxies de la
	 * page sont initialisés en une seule requête (anti N+1).
	 */
	@Query("""
			select d from DemandeTechnique d
			left join fetch d.client
			left join fetch d.agentAffecte
			where d.id in :ids
			""")
	List<DemandeTechnique> chargerConsultation(@Param("ids") Collection<Long> ids);
}
