package com.akrouty.gestiondemandes.security.presentation;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Réponse d'erreur de sécurité au format contrat API :
 * {@code code} stable + {@code message}. Aucun détail interne, aucune cause
 * cryptographique, aucune trace, aucun secret.
 *
 * <p>Le corps JSON est produit à partir de ces seules constantes (aucune
 * donnée utilisateur), sans dépendre d'un moteur de sérialisation.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErreurSecurite(String code, String message) {

	public static final ErreurSecurite AUTHENTIFICATION_REQUISE =
			new ErreurSecurite("AUTHENTIFICATION_REQUISE", "Authentification requise.");

	public static final ErreurSecurite ACCES_INTERDIT =
			new ErreurSecurite("ACCES_INTERDIT", "Accès interdit.");

	public static final ErreurSecurite AUTHENTIFICATION_ECHOUEE =
			new ErreurSecurite("AUTHENTIFICATION_ECHOUEE", "Authentification refusée.");

	/** Rendu JSON minimal {@code {"code":"...","message":"..."}}. */
	public String toJson() {
		return "{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}";
	}
}