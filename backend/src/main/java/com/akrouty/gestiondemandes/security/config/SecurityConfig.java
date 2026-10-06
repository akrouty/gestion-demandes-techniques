package com.akrouty.gestiondemandes.security.config;

import com.akrouty.gestiondemandes.identity.application.IdentiteService;
import com.akrouty.gestiondemandes.identity.domain.Role;
import com.akrouty.gestiondemandes.security.jwt.JwtService;
import com.akrouty.gestiondemandes.security.presentation.JsonAccessDeniedHandler;
import com.akrouty.gestiondemandes.security.presentation.JsonAuthenticationEntryPoint;
import com.akrouty.gestiondemandes.security.presentation.JwtAuthenticationFilter;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Chaîne de sécurité V1 stateless (ADR-005, SECURITY-DESIGN-V1).
 *
 * <ul>
 *   <li>session HTTP désactivée ({@code STATELESS}) : aucune {@code HttpSession}
 *       d'authentification, aucun cookie d'authentification ;</li>
 *   <li>login public, tout le reste authentifié ;</li>
 *   <li>RBAC : administration réservée à {@code ADMINISTRATEUR} ; barrières des
 *       routes métier du contrat API posées ici, sans faux contrôleur — les
 *       contrôleurs métier et contrôles contextuels arrivent au Bloc 3 ;</li>
 *   <li>filtre JWT qui relit l'utilisateur dans Identity à chaque requête ;</li>
 *   <li>CORS limité aux origines configurées ; CSRF justifié ci-dessous.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
public class SecurityConfig {

	@Bean
	JwtService jwtService(JwtProperties jwtProperties) {
		return new JwtService(jwtProperties);
	}

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			JwtService jwtService,
			IdentiteService identiteService) throws Exception {

		JsonAuthenticationEntryPoint entreePoint = new JsonAuthenticationEntryPoint();
		JsonAccessDeniedHandler refusAcces = new JsonAccessDeniedHandler();
		JwtAuthenticationFilter filtreJwt = new JwtAuthenticationFilter(jwtService, identiteService);

		http
				// CSRF désactivé car l'authentification est transportée EXCLUSIVEMENT par
				// l'en-tête Authorization: Bearer depuis une SPA qui garde le JWT en mémoire :
				// aucun cookie d'authentification n'est émis ni joint automatiquement par le
				// navigateur, donc aucune requête cross-site ne rejoue de credentials
				// (SECURITY-DESIGN-V1 §12, ADR-005 §10). Ce n'est pas la formule
				// « JWT => pas de CSRF » : si un cookie d'authentification est introduit un
				// jour, cette décision devra être réévaluée avant sa mise en service.
				.csrf(csrf -> csrf.disable())
				// Politique CORS explicite : origines configurées uniquement, aucun wildcard.
				.cors(cors -> {})
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				// Aucun mécanisme d'authentification alternatif (form login, basic, logout serveur).
				.formLogin(form -> form.disable())
				.httpBasic(basic -> basic.disable())
				.logout(logout -> logout.disable())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						// Login : public (aucun JWT requis).
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
						// Administration des utilisateurs : ADMINISTRATEUR exclusivement.
						.requestMatchers("/api/v1/utilisateurs/**").hasRole(Role.ADMINISTRATEUR.name())
						// ---- Barrières RBAC des routes métier du contrat API ----
						// Aucun contrôleur métier n'est créé ici : ces règles posent la
						// barrière de sécurité ; les contrôleurs et contrôles contextuels
						// (ex. Agent réellement affecté) sont implémentés au Bloc 3.
						// Responsable technique :
						.requestMatchers(HttpMethod.POST, "/api/v1/demandes")
								.hasRole(Role.RESPONSABLE_TECHNIQUE.name())
						.requestMatchers(HttpMethod.POST, "/api/v1/demandes/analyse-ia")
								.hasRole(Role.RESPONSABLE_TECHNIQUE.name())
						.requestMatchers(HttpMethod.PUT, "/api/v1/demandes/*/qualification")
								.hasRole(Role.RESPONSABLE_TECHNIQUE.name())
						.requestMatchers(HttpMethod.PUT, "/api/v1/demandes/*/affectation")
								.hasRole(Role.RESPONSABLE_TECHNIQUE.name())
						.requestMatchers(HttpMethod.POST, "/api/v1/demandes/*/cloture")
								.hasRole(Role.RESPONSABLE_TECHNIQUE.name())
						.requestMatchers(HttpMethod.POST, "/api/v1/demandes/*/refus-resolution")
								.hasRole(Role.RESPONSABLE_TECHNIQUE.name())
						.requestMatchers(HttpMethod.POST, "/api/v1/demandes/*/annulation")
								.hasRole(Role.RESPONSABLE_TECHNIQUE.name())
						.requestMatchers(HttpMethod.GET, "/api/v1/clients")
								.hasRole(Role.RESPONSABLE_TECHNIQUE.name())
						.requestMatchers(HttpMethod.GET, "/api/v1/agents")
								.hasRole(Role.RESPONSABLE_TECHNIQUE.name())
						// Agent technique :
						.requestMatchers(HttpMethod.POST, "/api/v1/demandes/*/demarrage-traitement")
								.hasRole(Role.AGENT_TECHNIQUE.name())
						.requestMatchers(HttpMethod.PATCH, "/api/v1/demandes/*/traitement")
								.hasRole(Role.AGENT_TECHNIQUE.name())
						.requestMatchers(HttpMethod.POST, "/api/v1/demandes/*/resolution")
								.hasRole(Role.AGENT_TECHNIQUE.name())
						// Consultation des demandes : RT ou AT — jamais l'ADMINISTRATEUR seul :
						.requestMatchers(HttpMethod.GET, "/api/v1/demandes")
								.hasAnyRole(
										Role.RESPONSABLE_TECHNIQUE.name(),
										Role.AGENT_TECHNIQUE.name())
						.requestMatchers(HttpMethod.GET, "/api/v1/demandes/*")
								.hasAnyRole(
										Role.RESPONSABLE_TECHNIQUE.name(),
										Role.AGENT_TECHNIQUE.name())
						.anyRequest().authenticated())
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(entreePoint)
						.accessDeniedHandler(refusAcces))
				.addFilterBefore(filtreJwt, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	/**
	 * CORS : origines issues de {@code CORS_ALLOWED_ORIGINS} uniquement.
	 * Une liste vide n'autorise aucune origine (aucun wildcard, aucune
	 * ouverture par défaut). {@code allowCredentials} reste désactivé :
	 * l'authentification n'utilise pas de cookie.
	 */
	@Bean
	CorsConfigurationSource corsConfigurationSource(CorsProperties proprietes) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(proprietes.originesNonNull());
		configuration.setAllowedMethods(List.of(
				HttpMethod.GET.name(),
				HttpMethod.POST.name(),
				HttpMethod.PUT.name(),
				HttpMethod.PATCH.name(),
				HttpMethod.DELETE.name(),
				HttpMethod.OPTIONS.name()));
		configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}