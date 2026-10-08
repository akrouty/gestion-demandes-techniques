package com.akrouty.gestiondemandes.request.persistence;

import com.akrouty.gestiondemandes.request.domain.Categorie;
import com.akrouty.gestiondemandes.request.domain.DemandeTechnique;
import com.akrouty.gestiondemandes.request.domain.Priorite;
import com.akrouty.gestiondemandes.request.domain.StatutDemande;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Composition des filtres de consultation des demandes (API-CONTRACT-V1 §9.2).
 *
 * <p>Les filtres sont TOUJOURS combinés à l'intérieur du périmètre déjà
 * autorisé : le périmètre de l'Agent est ajouté par l'application comme une
 * contrainte supplémentaire et ne peut jamais être élargi par un filtre.</p>
 *
 * <p>La recherche textuelle {@code recherche} est une recherche simple
 * case-insensitive sur {@code reference}, {@code titre} et {@code description} :
 * aucun moteur externe, aucune requête plein texte (API-CONTRACT-V1 §9.2).</p>
 */
public final class DemandeSpecifications {

	private DemandeSpecifications() {
	}

	public static Specification<DemandeTechnique> avecFiltres(
			StatutDemande statut,
			Priorite priorite,
			Categorie categorie,
			Long clientId,
			Long agentId,
			String recherche) {
		return (root, query, cb) -> {
			List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
			if (statut != null) {
				predicates.add(cb.equal(root.get("statut"), statut));
			}
			if (priorite != null) {
				predicates.add(cb.equal(root.get("priorite"), priorite));
			}
			if (categorie != null) {
				predicates.add(cb.equal(root.get("categorie"), categorie));
			}
			if (clientId != null) {
				predicates.add(cb.equal(root.get("client").get("id"), clientId));
			}
			if (agentId != null) {
				predicates.add(cb.equal(root.get("agentAffecte").get("id"), agentId));
			}
			if (recherche != null && !recherche.isBlank()) {
				String motif = "%" + recherche.toLowerCase() + "%";
				predicates.add(cb.or(
						cb.like(cb.lower(root.get("reference")), motif),
						cb.like(cb.lower(root.get("titre")), motif),
						cb.like(cb.lower(root.get("description")), motif)));
			}
			return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
		};
	}

	/**
	 * Périmètre de l'Agent : seules les demandes dont il est l'Agent affecté.
	 * Toujours combiné par ET avec les filtres — jamais remplacé par eux.
	 */
	public static Specification<DemandeTechnique> perimetreAgent(Long agentId) {
		return (root, query, cb) -> cb.equal(root.get("agentAffecte").get("id"), agentId);
	}
}