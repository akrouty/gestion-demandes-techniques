package com.akrouty.gestiondemandes.request.presentation;

import com.akrouty.gestiondemandes.request.application.DemandeService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Liste des Agents affectables (API-CONTRACT-V1, GET /agents) : uniquement
 * les utilisateurs actifs possédant {@code AGENT_TECHNIQUE}, via la
 * responsabilité publique d'Identity. Aucun credential exposé.
 */
@RestController
@RequestMapping("/api/v1/agents")
public class AgentController {

	private final DemandeService service;

	public AgentController(DemandeService service) {
		this.service = service;
	}

	@GetMapping
	public List<AgentAssignableResponse> lister() {
		return service.listerAgents().stream()
				.map(a -> new AgentAssignableResponse(a.id(), a.nom(), a.email(), a.actif()))
				.toList();
	}
}