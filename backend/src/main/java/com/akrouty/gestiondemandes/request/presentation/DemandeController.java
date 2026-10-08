package com.akrouty.gestiondemandes.request.presentation;

import com.akrouty.gestiondemandes.identity.domain.Role;
import com.akrouty.gestiondemandes.request.application.CreationDemandeCommand;
import com.akrouty.gestiondemandes.request.application.DemandeConsultation;
import com.akrouty.gestiondemandes.request.application.DemandeResumeConsultation;
import com.akrouty.gestiondemandes.request.application.DemandeService;
import com.akrouty.gestiondemandes.request.application.FiltresDemande;
import com.akrouty.gestiondemandes.request.application.RequeteInvalidException;
import com.akrouty.gestiondemandes.request.application.TraitementCommand;
import com.akrouty.gestiondemandes.request.domain.Categorie;
import com.akrouty.gestiondemandes.request.domain.Priorite;
import com.akrouty.gestiondemandes.request.domain.StatutDemande;
import com.akrouty.gestiondemandes.security.application.UtilisateurAuthentifie;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Frontière HTTP des demandes (API-CONTRACT-V1). Le RBAC est appliqué par la
 * chaîne de sécurité (Bloc 2) ; cette couche ne fait que la validation de
 * forme, l'extraction de l'identité authentifiée et l'appel à
 * {@link DemandeService} — aucune logique métier.
 */
@RestController
@RequestMapping("/api/v1/demandes")
public class DemandeController {

	/** Tri par défaut imposé par API-CONTRACT-V1 §9.2. */
	private static final String TRI_PAR_DEFAUT_CHAMP = "dateCreation";
	private static final Sort.Direction DIRECTION_PAR_DEFAUT = Sort.Direction.DESC;

	/** Champs de tri sûrs : uniquement des propriétés réellement exposées. */
	private static final Set<String> CHAMPS_TRI_AUTORISES = Set.of(
			"dateCreation", "dateModification", "reference", "titre", "statut", "categorie", "priorite");

	private final DemandeService service;

	public DemandeController(DemandeService service) {
		this.service = service;
	}

	// ---------------------------------------------------------------- création

	@PostMapping
	public ResponseEntity<DemandeDetailResponse> creer(@Valid @RequestBody CreationDemandeRequest request) {
		CreationDemandeCommand command = new CreationDemandeCommand(
				request.titre(),
				request.description(),
				request.categorie(),
				request.priorite(),
				request.clientId(),
				request.nouveauClient() == null
						? null
						: new CreationDemandeCommand.NouveauClientCommand(
								request.nouveauClient().nom(),
								request.nouveauClient().email(),
								request.nouveauClient().telephone()));
		DemandeConsultation consultation = service.creer(acteurId(), command);
		return ResponseEntity
				.created(URI.create("/api/v1/demandes/" + consultation.reference()))
				.body(verserDetail(consultation));
	}

	// ---------------------------------------------------------------- consultations

	@GetMapping
	public PageDemandesResponse lister(
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "20") int size,
			@RequestParam(name = "sort", required = false) String sort,
			@RequestParam(name = "statut", required = false) StatutDemande statut,
			@RequestParam(name = "priorite", required = false) Priorite priorite,
			@RequestParam(name = "categorie", required = false) Categorie categorie,
			@RequestParam(name = "clientId", required = false) Long clientId,
			@RequestParam(name = "agentId", required = false) Long agentId,
			@RequestParam(name = "recherche", required = false) String recherche) {
		FiltresDemande filtres = new FiltresDemande(statut, priorite, categorie, clientId, agentId, recherche);
		Page<DemandeResumeConsultation> resultat = service.lister(
				acteurId(), acteurRoles(), filtres, construirePageable(page, size, sort));
		return new PageDemandesResponse(
				resultat.getContent().stream().map(DemandeController::verserResume).toList(),
				resultat.getNumber(),
				resultat.getSize(),
				resultat.getTotalElements(),
				resultat.getTotalPages());
	}

	@GetMapping("/{reference}")
	public DemandeDetailResponse obtenir(@PathVariable String reference) {
		return verserDetail(service.consulter(acteurId(), acteurRoles(), reference));
	}
	// ---------------------------------------------------------------- actions métier

	@PutMapping("/{reference}/qualification")
	public DemandeDetailResponse qualifier(
			@PathVariable String reference, @Valid @RequestBody QualificationDemandeRequest request) {
		return verserDetail(service.qualifier(acteurId(), reference, request.categorie(), request.priorite()));
	}

	@PutMapping("/{reference}/affectation")
	public DemandeDetailResponse affecter(
			@PathVariable String reference, @Valid @RequestBody AffectationDemandeRequest request) {
		return verserDetail(service.affecter(acteurId(), reference, request.agentId()));
	}

	@PostMapping("/{reference}/demarrage-traitement")
	public DemandeDetailResponse demarrerTraitement(@PathVariable String reference) {
		return verserDetail(service.demarrerTraitement(acteurId(), reference));
	}

	@PatchMapping("/{reference}/traitement")
	public DemandeDetailResponse majTraitement(
			@PathVariable String reference, @RequestBody TraitementDemandeRequest request) {
		boolean descriptionFournie = request.isDescriptionTraitementFournie();
		boolean solutionFournie = request.isSolutionFournie();
		if (!descriptionFournie && !solutionFournie) {
			throw new RequeteInvalidException(
					"Au moins un champ (descriptionTraitement ou solution) doit être fourni.");
		}
		if (descriptionFournie
				&& (request.getDescriptionTraitement() == null || request.getDescriptionTraitement().isBlank())) {
			throw new RequeteInvalidException("descriptionTraitement fourni doit être non vide.");
		}
		if (solutionFournie && (request.getSolution() == null || request.getSolution().isBlank())) {
			throw new RequeteInvalidException("solution fourni doit être non vide.");
		}
		return verserDetail(service.majTraitement(acteurId(), reference, new TraitementCommand(
				request.getDescriptionTraitement(), descriptionFournie,
				request.getSolution(), solutionFournie)));
	}

	@PostMapping("/{reference}/resolution")
	public DemandeDetailResponse resoudre(@PathVariable String reference) {
		return verserDetail(service.resoudre(acteurId(), reference));
	}

	@PostMapping("/{reference}/refus-resolution")
	public DemandeDetailResponse refuserResolution(@PathVariable String reference) {
		return verserDetail(service.refuserResolution(acteurId(), reference));
	}

	@PostMapping("/{reference}/cloture")
	public DemandeDetailResponse cloturer(@PathVariable String reference) {
		return verserDetail(service.cloturer(acteurId(), reference));
	}

	@PostMapping("/{reference}/annulation")
	public DemandeDetailResponse annuler(
			@PathVariable String reference, @Valid @RequestBody AnnulationDemandeRequest request) {
		return verserDetail(service.annuler(acteurId(), reference, request.motif()));
	}
	// ---------------------------------------------------------------- privé

	private Pageable construirePageable(int page, int size, String sort) {
		if (page < 0 || size < 1) {
			throw new ParametreInvalidException("Paramètres de pagination invalides");
		}
		if (sort == null || sort.isBlank()) {
			return PageRequest.of(page, size, Sort.by(DIRECTION_PAR_DEFAUT, TRI_PAR_DEFAUT_CHAMP));
		}
		String[] parts = sort.split(",", -1);
		if (parts.length > 2) {
			throw new ParametreInvalidException("Format de tri invalide");
		}
		String champ = parts[0].trim();
		if (!CHAMPS_TRI_AUTORISES.contains(champ)) {
			throw new ParametreInvalidException("Champ de tri invalide");
		}
		Sort.Direction direction = DIRECTION_PAR_DEFAUT;
		if (parts.length > 1) {
			String valeur = parts[1].trim();
			if (!"asc".equalsIgnoreCase(valeur) && !"desc".equalsIgnoreCase(valeur)) {
				throw new ParametreInvalidException("Direction de tri invalide");
			}
			direction = "desc".equalsIgnoreCase(valeur) ? Sort.Direction.DESC : Sort.Direction.ASC;
		}
		return PageRequest.of(page, size, Sort.by(direction, champ));
	}

	/** Identité extraite de Spring Security (présentation → application, jamais l'inverse). */
	private Long acteurId() {
		return acteur().id();
	}

	private Set<Role> acteurRoles() {
		return acteur().roles();
	}

	private UtilisateurAuthentifie acteur() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof UtilisateurAuthentifie)) {
			throw new AccessDeniedException("Utilisateur authentifié absent");
		}
		return (UtilisateurAuthentifie) authentication.getPrincipal();
	}

	private static DemandeDetailResponse verserDetail(DemandeConsultation c) {
		return new DemandeDetailResponse(
				c.reference(), c.titre(), c.description(), c.categorie(), c.priorite(), c.statut(),
				verserClient(c.client()), verserUtilisateur(c.createur()), verserUtilisateur(c.agentAffecte()),
				c.descriptionTraitement(), c.solution(), c.motifAnnulation(),
				c.dateCreation(), c.dateModification(), c.dateResolution(), c.dateCloture(), c.dateAnnulation());
	}

	private static DemandeSummaryResponse verserResume(DemandeResumeConsultation c) {
		return new DemandeSummaryResponse(
				c.reference(), c.titre(), c.categorie(), c.priorite(), c.statut(),
				verserClient(c.client()), verserUtilisateur(c.agentAffecte()),
				c.dateCreation(), c.dateModification());
	}

	private static ClientSummaryResponse verserClient(
			com.akrouty.gestiondemandes.request.application.ClientConsultation c) {
		return new ClientSummaryResponse(c.id(), c.nom(), c.email(), c.telephone());
	}

	private static UtilisateurResumeResponse verserUtilisateur(
			com.akrouty.gestiondemandes.identity.application.UtilisateurConsultation u) {
		if (u == null) {
			return null;
		}
		return new UtilisateurResumeResponse(u.id(), u.nom(), u.email(), u.actif(), u.roles());
	}
}
