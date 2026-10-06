package com.akrouty.gestiondemandes.security.presentation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * {@code 401} explicite pour toute authentification absente ou invalide
 * (JWT manquant, malformé, altéré, expiré, mauvais issuer, mauvais
 * algorithme, compte absent ou désactivé).
 *
 * <p>La réponse ne révèle ni la cause technique, ni l'existence d'un
 * utilisateur, ni aucun détail cryptographique. Le corps est écrit
 * directement à partir des seules constantes stables d'{@link ErreurSecurite}.</p>
 */
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

	@Override
	public void commence(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		erreur(response, HttpServletResponse.SC_UNAUTHORIZED, ErreurSecurite.AUTHENTIFICATION_REQUISE);
	}

	static void erreur(HttpServletResponse response, int statut, ErreurSecurite erreur) throws IOException {
		response.setStatus(statut);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.getWriter().write(erreur.toJson());
	}
}