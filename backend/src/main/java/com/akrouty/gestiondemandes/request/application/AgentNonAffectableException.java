package com.akrouty.gestiondemandes.request.application;

/**
 * Utilisateur cible existant mais non affectable comme Agent : inactif ou
 * dépourvu du rôle {@code AGENT_TECHNIQUE}. Traduit en {@code 409 Conflict}
 * par presentation (l'inexistant, lui, produit {@code 404}).
 */
public class AgentNonAffectableException extends RuntimeException {

	public AgentNonAffectableException(String message) {
		super(message);
	}
}
