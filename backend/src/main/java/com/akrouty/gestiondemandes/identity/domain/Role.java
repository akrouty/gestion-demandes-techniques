package com.akrouty.gestiondemandes.identity.domain;

/**
 * Rôles applicatifs validés.
 *
 * <p>Le rôle est persisté comme code textuel stable dans la table de collection
 * {@code utilisateur_role}. {@code Role} n'est pas une entité JPA.</p>
 */
public enum Role {

	RESPONSABLE_TECHNIQUE,
	AGENT_TECHNIQUE,
	ADMINISTRATEUR
}
