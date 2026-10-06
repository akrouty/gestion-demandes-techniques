package com.akrouty.gestiondemandes.identity.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Politique minimale de mot de passe, entièrement externalisée.
 *
 * <p>Aucune valeur n'est codée en dur dans le code source : la configuration
 * provient de {@code PASSWORD_MIN_LENGTH} et {@code BCRYPT_STRENGTH}
 * (cf. {@code application.properties}). Une configuration absente ou invalide
 * provoque un échec explicite de démarrage. La longueur minimale est le seul
 * critère validé : aucune règle de complexité (majuscule, chiffre, caractère
 * spécial) n'est imposée faute de décision validée.</p>
 *
 * @param minLength      longueur minimale du mot de passe (octets de la chaîne, sans troncature)
 * @param bcryptStrength facteur de coût BCrypt (mesuré sur l'environnement cible avant production)
 */
@ConfigurationProperties(prefix = "app.password")
public record MotDePasseProperties(int minLength, int bcryptStrength) {

	public void valider() {
		if (minLength < 1) {
			throw new IllegalStateException(
					"PASSWORD_MIN_LENGTH invalide : une longueur minimale positive est obligatoire");
		}
		if (bcryptStrength < 4 || bcryptStrength > 31) {
			throw new IllegalStateException(
					"BCRYPT_STRENGTH invalide : le facteur de coût BCrypt doit être compris entre 4 et 31");
		}
	}
}