package com.akrouty.gestiondemandes.identity.presentation;

import com.akrouty.gestiondemandes.identity.application.AutoDesactivationInterditeException;
import com.akrouty.gestiondemandes.identity.application.EmailDejaUtiliseException;
import com.akrouty.gestiondemandes.identity.application.MotDePasseNonConformeException;
import com.akrouty.gestiondemandes.identity.application.UtilisateurIntrouvableException;
import com.akrouty.gestiondemandes.identity.domain.RoleAdministrateurNonAttribuableException;
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
 * Mapping des erreurs d'administration vers les codes HTTP du contrat API.
 *
 * <p>Aucune exception interne, stack trace, SQL ou nom de contrainte de base
 * n'est exposé au client.</p>
 */
@RestControllerAdvice(assignableTypes = UtilisateurController.class)
public class UtilisateurApiExceptionHandler {

	@ExceptionHandler(AutoDesactivationInterditeException.class)
	public ResponseEntity<ErreurApi> autoDesactivation(AutoDesactivationInterditeException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ErreurApi.simple("AUTO_DESACTIVATION_INTERDITE", e.getMessage()));
	}

	@ExceptionHandler(UtilisateurIntrouvableException.class)
	public ResponseEntity<ErreurApi> introuvable(UtilisateurIntrouvableException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ErreurApi.simple("UTILISATEUR_INTROUVABLE", "Utilisateur introuvable."));
	}

	@ExceptionHandler(EmailDejaUtiliseException.class)
	public ResponseEntity<ErreurApi> conflit(EmailDejaUtiliseException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ErreurApi.simple("EMAIL_DEJA_UTILISE", "Cet email est déjà utilisé."));
	}

	@ExceptionHandler(RoleAdministrateurNonAttribuableException.class)
	public ResponseEntity<ErreurApi> roleInterdit(RoleAdministrateurNonAttribuableException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ErreurApi.simple("ROLE_METIER_INVALIDE",
						"Seuls les rôles métier RESPONSABLE_TECHNIQUE et AGENT_TECHNIQUE sont admis."));
	}

	@ExceptionHandler(MotDePasseNonConformeException.class)
	public ResponseEntity<ErreurApi> motDePasse(MotDePasseNonConformeException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ErreurApi.simple("MOT_DE_PASSE_INVALIDE", "Le mot de passe ne respecte pas la politique minimale."));
	}

	@ExceptionHandler(ParametreInvalidException.class)
	public ResponseEntity<ErreurApi> parametre(ParametreInvalidException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ErreurApi.simple("PARAMETRE_INVALIDE", "Paramètre de requête invalide."));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErreurApi> validation(MethodArgumentNotValidException e) {
		List<ErreurApi.ChampErreur> champs = e.getBindingResult().getFieldErrors().stream()
				.map(UtilisateurApiExceptionHandler::versChampErreur)
				.toList();
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ErreurApi("VALIDATION", "Requête invalide.", champs));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErreurApi> corpsIllisible(HttpMessageNotReadableException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ErreurApi.simple("REQUETE_INVALIDE", "Corps de requête invalide."));
	}

	/** Course possible sur l'unicité de l'email : 409 générique, sans détail de contrainte. */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErreurApi> integrite(DataIntegrityViolationException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ErreurApi.simple("CONFLIT", "Conflit avec l'état actuel des données."));
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