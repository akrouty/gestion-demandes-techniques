package com.akrouty.gestiondemandes.security.presentation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * {@code 403} pour un utilisateur authentifié dont le rôle est insuffisant.
 * Aucun détail sur les rôles attendus ni sur la ressource n'est révélé.
 */
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

	@Override
	public void handle(
			HttpServletRequest request,
			HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		JsonAuthenticationEntryPoint.erreur(
				response, HttpServletResponse.SC_FORBIDDEN, ErreurSecurite.ACCES_INTERDIT);
	}
}