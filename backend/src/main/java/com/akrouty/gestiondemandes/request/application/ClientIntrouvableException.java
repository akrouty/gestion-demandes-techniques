package com.akrouty.gestiondemandes.request.application;

/** Client ciblé introuvable. Traduit en {@code 404 Not Found} par presentation. */
public class ClientIntrouvableException extends RuntimeException {

	public ClientIntrouvableException(Long clientId) {
		super("Client introuvable : " + clientId);
	}
}
