package com.akrouty.gestiondemandes.identity.domain;

/**
 * Refus d'attribuer ou de retirer le rôle {@code ADMINISTRATEUR} depuis les
 * opérations d'administration des rôles métier ({@code POST /utilisateurs},
 * {@code PUT /utilisateurs/{id}/roles-metier}).
 *
 * <p>Traduit en {@code 400 Bad Request} à la frontière REST.</p>
 */
public class RoleAdministrateurNonAttribuableException extends RuntimeException {

	public RoleAdministrateurNonAttribuableException() {
		super("Le rôle ADMINISTRATEUR ne peut être ni attribué ni retiré par cette opération");
	}
}