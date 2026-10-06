package com.akrouty.gestiondemandes.security.presentation;

import com.akrouty.gestiondemandes.identity.application.IdentiteService;
import com.akrouty.gestiondemandes.identity.application.UtilisateurConsultation;
import com.akrouty.gestiondemandes.security.application.UtilisateurAuthentifie;
import com.akrouty.gestiondemandes.security.jwt.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Réalise la décision VALIDATED de relecture Identity à CHAQUE requête
 * protégée (ADR-005 §3, SECURITY-DESIGN-V1 §3) :
 *
 * <pre>
 * JWT valide → sub → IdentiteService → relecture Utilisateur en base
 *   → existe ? → actif ? → rôles ACTUELS → Authentication Spring Security → RBAC
 * </pre>
 *
 * <p>Les rôles portés par un ancien JWT ne font jamais autorité : un rôle
 * retiré est perdu dès la requête suivante, un compte désactivé est refusé
 * dès la requête suivante. Le token n'est ni journalisé ni mis en cache.</p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER = "Bearer ";

	private final JwtService jwtService;
	private final IdentiteService identiteService;

	public JwtAuthenticationFilter(JwtService jwtService, IdentiteService identiteService) {
		this.jwtService = jwtService;
		this.identiteService = identiteService;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		String entete = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (entete == null || !entete.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
			filterChain.doFilter(request, response);
			return;
		}
		String jeton = entete.substring(BEARER.length()).trim();

		try {
			appliquerIdentiteLue(jeton, request);
		} catch (RuntimeException e) {
			// Échec générique : aucun détail (cause crypto, existence, trace)
			// n'est propagé — le point d'entrée renverra un 401 neutre.
			SecurityContextHolder.clearContext();
		}
		filterChain.doFilter(request, response);
	}

	private void appliquerIdentiteLue(String jeton, HttpServletRequest request) {
		var jwtValide = jwtService.valider(jeton);

		// sub : identifiant technique, jamais une autorité en soi.
		Long id = Long.valueOf(jwtValide.getSubject());

		// Relecture systématique depuis Identity : existence, actif, rôles actuels.
		UtilisateurConsultation identite = identiteService.chargerIdentite(id).orElse(null);
		if (identite == null || !identite.actif()) {
			SecurityContextHolder.clearContext();
			return;
		}

		UtilisateurAuthentifie principal =
				new UtilisateurAuthentifie(identite.id(), identite.email(), identite.roles());
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				principal, null, principal.autorites());
		authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}
}