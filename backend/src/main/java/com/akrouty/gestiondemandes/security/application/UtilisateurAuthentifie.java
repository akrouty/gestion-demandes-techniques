package com.akrouty.gestiondemandes.security.application;

import com.akrouty.gestiondemandes.identity.domain.Role;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Représentation minimale de l'utilisateur authentifié sur une requête
 * (principal Spring Security).
 *
 * <p>Construite à CHAQUE requête protégée depuis les rôles actuels relus en
 * base par Identity — jamais depuis les claims du JWT. Elle ne contient
 * aucun {@code passwordHash} ni donnée métier. Aucune {@code HttpSession}
 * n'est utilisée (API stateless).</p>
 *
 * @param id    identifiant technique de l'utilisateur
 * @param email email normalisé
 * @param roles rôles ACTUELS lus en base pour cette requête
 */
public record UtilisateurAuthentifie(Long id, String email, Set<Role> roles) {

	/** Union des rôles actuels en autorités Spring Security ({@code ROLE_*}). */
	public Collection<GrantedAuthority> autorites() {
		return roles.stream()
				.map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.name()))
				.toList();
	}
}