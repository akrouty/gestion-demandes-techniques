package com.akrouty.gestiondemandes.request.presentation;

import com.akrouty.gestiondemandes.identity.application.UtilisateurIntrouvableException;
import com.akrouty.gestiondemandes.request.application.AccesDemandeInterditException;
import com.akrouty.gestiondemandes.request.application.AgentNonAffectableException;
import com.akrouty.gestiondemandes.request.application.ClientIntrouvableException;
import com.akrouty.gestiondemandes.request.application.DemandeIntrouvableException;
import com.akrouty.gestiondemandes.request.application.RequeteInvalidException;
import com.akrouty.gestiondemandes.request.domain.DemandeTermineeException;
import com.akrouty.gestiondemandes.request.domain.SolutionRequiseException;
import com.akrouty.gestiondemandes.request.domain.TransitionInvalidException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Mapping des exceptions métier du module {@code request} vers les codes HTTP
 * d'API-CONTRACT-V1 §7. Le domaine ne connaît aucun code HTTP.
 *
 * <p>Aucune exception interne, stack trace, SQL ou nom de contrainte de base
 * n'est exposé au client.</p>
 */
@RestControllerAdvice(assignableTypes = {
		DemandeController.class, ClientController.class, AgentController.class})
public class DemandeApiExceptionHandler {

	// ---------------------------------------------------------------- 404

	@ExceptionHandler(DemandeIntrouvableException.class)
	public ResponseEntity<ErreurApi> demandeIntrouvable(DemandeIntrouvableException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ErreurApi.simple("DEMANDE_INTROUVABLE", "Demande introuvable."));
	}

	@ExceptionHandler(ClientIntrouvableException.class)
	public ResponseEntity<ErreurApi> clientIntrouvable(ClientIntrouvableException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ErreurApi.simple("CLIENT_INTROUVABLE", "Client introuvable."));
	}

	@ExceptionHandler(UtilisateurIntrouvableException.class)
	public ResponseEntity<ErreurApi> utilisateurIntrouvable(UtilisateurIntrouvableException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ErreurApi.simple("UTILISATEUR_INTROUVABLE", "Utilisateur introuvable."));
	}

	// ---------------------------------------------------------------- 403

	@ExceptionHandler(AccesDemandeInterditException.class)
	public ResponseEntity<ErreurApi> accesInterdit(AccesDemandeInterditException e) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(ErreurApi.simple("ACCES_INTERDIT", "Accès interdit."));
	}

	// ---------------------------------------------------------------- 409

	@ExceptionHandler(DemandeTermineeException.class)
	public ResponseEntity<ErreurApi> demandeTerminee(DemandeTermineeException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ErreurApi.simple("DEMANDE_TERMINEE",
						"La demande est terminée et n'est plus modifiable."));
	}

	@ExceptionHandler(TransitionInvalidException.class)
	public ResponseEntity<ErreurApi> transitionInvalid(TransitionInvalidException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ErreurApi.simple("TRANSITION_INVALIDE",
						"L'état actuel de la demande ne permet pas cette opération."));
	}

	@ExceptionHandler(SolutionRequiseException.class)
	public ResponseEntity<ErreurApi> solutionRequise(SolutionRequiseException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ErreurApi.simple("SOLUTION_REQUISE",
						"Une solution est requise avant la résolution."));
	}

	@ExceptionHandler(AgentNonAffectableException.class)
	public ResponseEntity<ErreurApi> agentNonAffectable(AgentNonAffectableException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ErreurApi.simple("AGENT_NON_AFFECTABLE",
						"L'utilisateur cible doit être actif et posséder le rôle AGENT_TECHNIQUE."));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErreurApi> integrite(DataIntegrityViolationException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ErreurApi.simple("CONFLIT", "Conflit avec l'état actuel des données."));
	}
	// ---------------------------------------------------------------- 400

	@ExceptionHandler(RequeteInvalidException.class)
	public ResponseEntity<ErreurApi> requeteInvalid(RequeteInvalidException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ErreurApi.simple("REQUETE_INVALIDE", e.getMessage()));
	}

	@ExceptionHandler(ParametreInvalidException.class)
	public ResponseEntity<ErreurApi> parametre(ParametreInvalidException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ErreurApi.simple("PARAMETRE_INVALIDE", "Paramètre de requête invalide."));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErreurApi> validation(MethodArgumentNotValidException e) {
		List<ErreurApi.ChampErreur> champs = e.getBindingResult().getFieldErrors().stream()
				.map(DemandeApiExceptionHandler::versChampErreur)
				.toList();
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ErreurApi("VALIDATION", "Requête invalide.", champs));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErreurApi> corpsIllisible(HttpMessageNotReadableException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ErreurApi.simple("REQUETE_INVALIDE", "Corps de requête invalide."));
	}

	/** Enum ou identifiant de paramètre incorrect (filtres de consultation). */
	@ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErreurApi> typeIncorrect(
			org.springframework.web.method.annotation.MethodArgumentTypeMismatchException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ErreurApi.simple("PARAMETRE_INVALIDE", "Paramètre de requête invalide."));
	}

	private static ErreurApi.ChampErreur versChampErreur(FieldError erreur) {
		String code = switch (erreur.getCode() == null ? "" : erreur.getCode()) {
			case "NotBlank", "NotNull", "NotEmpty" -> "OBLIGATOIRE";
			case "Email" -> "EMAIL_INVALIDE";
			default -> "INVALIDE";
		};
		return new ErreurApi.ChampErreur(erreur.getField(), code, erreur.getDefaultMessage());
	}
}