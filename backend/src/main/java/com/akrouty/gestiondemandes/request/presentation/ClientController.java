package com.akrouty.gestiondemandes.request.presentation;

import com.akrouty.gestiondemandes.request.application.ClientConsultation;
import com.akrouty.gestiondemandes.request.application.DemandeService;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recherche simple des clients existants pour la sélection à la création
 * d'une demande (API-CONTRACT-V1 §3, GET /clients).
 *
 * <p>Aucun droit de création, modification ou suppression autonome : le
 * Client reste géré uniquement dans le contexte d'une demande. Aucun
 * endpoint POST/PUT/DELETE.</p>
 */
@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {

	/** Champs de tri sûrs. */
	private static final Set<String> CHAMPS_TRI_AUTORISES = Set.of("id", "nom", "email", "telephone");

	private final DemandeService service;

	public ClientController(DemandeService service) {
		this.service = service;
	}

	@GetMapping
	public PageClientsResponse lister(
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "20") int size,
			@RequestParam(name = "sort", required = false) String sort,
			@RequestParam(name = "recherche", required = false) String recherche) {
		Page<ClientConsultation> resultat = service.listerClients(recherche, construirePageable(page, size, sort));
		return new PageClientsResponse(
				resultat.getContent().stream()
						.map(c -> new ClientSummaryResponse(c.id(), c.nom(), c.email(), c.telephone()))
						.toList(),
				resultat.getNumber(),
				resultat.getSize(),
				resultat.getTotalElements(),
				resultat.getTotalPages());
	}

	private Pageable construirePageable(int page, int size, String sort) {
		if (page < 0 || size < 1) {
			throw new ParametreInvalidException("Paramètres de pagination invalides");
		}
		if (sort == null || sort.isBlank()) {
			return PageRequest.of(page, size);
		}
		String[] parts = sort.split(",");
		String champ = parts[0].trim();
		if (!CHAMPS_TRI_AUTORISES.contains(champ)) {
			throw new ParametreInvalidException("Champ de tri invalide");
		}
		Sort.Direction direction = Sort.Direction.ASC;
		if (parts.length > 1) {
			String valeur = parts[1].trim();
			if (!"asc".equalsIgnoreCase(valeur) && !"desc".equalsIgnoreCase(valeur)) {
				throw new ParametreInvalidException("Direction de tri invalide");
			}
			direction = "desc".equalsIgnoreCase(valeur) ? Sort.Direction.DESC : Sort.Direction.ASC;
		}
		return PageRequest.of(page, size, Sort.by(direction, champ));
	}
}