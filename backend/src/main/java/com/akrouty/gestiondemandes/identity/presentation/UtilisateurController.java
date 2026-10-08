package com.akrouty.gestiondemandes.identity.presentation;

import com.akrouty.gestiondemandes.identity.application.CreationUtilisateurCommand;
import com.akrouty.gestiondemandes.identity.application.IdentiteService;
import com.akrouty.gestiondemandes.identity.application.UtilisateurConsultation;
import com.akrouty.gestiondemandes.identity.domain.Role;
import com.akrouty.gestiondemandes.security.application.UtilisateurAuthentifie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Frontière HTTP d'administration des utilisateurs (API-CONTRACT-V1).
 *
 * <p>Tous les endpoints exigent le rôle {@code ADMINISTRATEUR} (RBAC appliqué
 * par la chaîne de sécurité) et l'authentification JWT. Aucun endpoint de
 * suppression d'utilisateur n'existe en V1.</p>
 */
@RestController
@RequestMapping("/api/v1/utilisateurs")
public class UtilisateurController {

	/** Tri autorisé : aucun ordre par défaut n'est imposé (non décidé au contrat). */
	private static final Set<String> CHAMPS_TRI_AUTORISES = Set.of("id", "nom", "email", "actif");

	private final IdentiteService identite;

	public UtilisateurController(IdentiteService identite) {
		this.identite = identite;
	}

	@GetMapping
	public PageUtilisateursResponse lister(
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "20") int size,
			@RequestParam(name = "sort", required = false) String sort) {
		Page<UtilisateurConsultation> resultat = identite.lister(construirePageable(page, size, sort));
		return new PageUtilisateursResponse(
				resultat.getContent().stream().map(UtilisateurController::versSummary).toList(),
				resultat.getNumber(),
				resultat.getSize(),
				resultat.getTotalElements(),
				resultat.getTotalPages());
	}

	@GetMapping("/{id}")
	public UtilisateurDetailResponse obtenir(@PathVariable Long id) {
		return versDetail(identite.obtenir(id));
	}

	@PostMapping
	public ResponseEntity<UtilisateurDetailResponse> creer(@Valid @RequestBody CreationUtilisateurRequest request) {
		UtilisateurConsultation cree = identite.creer(new CreationUtilisateurCommand(
				request.nom(),
				request.email(),
				request.actif(),
				request.rolesMetier() == null ? Set.of() : Set.copyOf(request.rolesMetier()),
				request.password()));
		return ResponseEntity
				.created(URI.create("/api/v1/utilisateurs/" + cree.id()))
				.body(versDetail(cree));
	}

	@PutMapping("/{id}")
	public UtilisateurDetailResponse modifier(
			@PathVariable Long id,
			@Valid @RequestBody ModificationUtilisateurRequest request) {
		return versDetail(identite.modifier(id, request.nom(), request.email()));
	}

	@PostMapping("/{id}/activation")
	public UtilisateurDetailResponse activer(@PathVariable Long id) {
		return versDetail(identite.activer(id));
	}

	@PostMapping("/{id}/desactivation")
	public UtilisateurDetailResponse desactiver(@PathVariable Long id,
			@AuthenticationPrincipal UtilisateurAuthentifie acteur) {
		return versDetail(identite.desactiver(id, acteur.id()));
	}

	@PutMapping("/{id}/roles-metier")
	public UtilisateurDetailResponse remplacerRolesMetier(
			@PathVariable Long id,
			@RequestBody RolesMetierRequest request) {
		Set<Role> rolesMetier = (request == null || request.rolesMetier() == null)
				? Set.of()
				: Set.copyOf(request.rolesMetier());
		return versDetail(identite.remplacerRolesMetier(id, rolesMetier));
	}

	// ---------------------------------------------------------------- privé

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

	private static UtilisateurDetailResponse versDetail(UtilisateurConsultation u) {
		return new UtilisateurDetailResponse(u.id(), u.nom(), u.email(), u.actif(), u.roles());
	}

	private static UtilisateurSummaryResponse versSummary(UtilisateurConsultation u) {
		return new UtilisateurSummaryResponse(u.id(), u.nom(), u.email(), u.actif(), u.roles());
	}
}