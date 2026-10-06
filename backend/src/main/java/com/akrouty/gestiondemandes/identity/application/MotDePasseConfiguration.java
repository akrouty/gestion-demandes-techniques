package com.akrouty.gestiondemandes.identity.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration du hachage des mots de passe du module {@code identity}.
 *
 * <p>Le {@code PasswordEncoder} est une responsabilité d'{@code identity}
 * (le mot de passe est une donnée d'identité) : il est déclaré ici pour que
 * la création (hachage) et le module {@code security} (vérification du login)
 * partagent exactement le même encodeur, sans que {@code identity} dépende du
 * module {@code security}.</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MotDePasseProperties.class)
public class MotDePasseConfiguration {

	@Bean
	PasswordEncoder passwordEncoder(MotDePasseProperties proprietes) {
		proprietes.valider();
		return new BCryptPasswordEncoder(proprietes.bcryptStrength());
	}
}