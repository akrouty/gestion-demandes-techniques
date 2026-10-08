package com.akrouty.gestiondemandes.request.application;

import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Génération technique de la référence d'une demande (décision V1).
 *
 * <p>Stratégie minimale : UUID v4 généré côté serveur. La référence :</p>
 *
 * <ul>
 *   <li>est générée par le backend, jamais fournie par le client ;</li>
 *   <li>est distincte de l'id technique ;</li>
 *   <li>est obligatoire, unique (contrainte DB existante) et immuable ;</li>
 *   <li>est utilisée dans les URLs.</li>
 * </ul>
 *
 * <p>Aucun compteur métier, année, préfixe commercial ou séquence
 * PostgreSQL supplémentaire (le format significatif reste différé).</p>
 */
@Component
public class GenerateurReferenceDemande {

	/** Vérifie aussi l'unicité pour écarter toute collision (quasi impossible). */
	public String generer(UniciteReferenceVerificatrice verificateur) {
		String reference;
		do {
			reference = UUID.randomUUID().toString();
		} while (verificateur.dejaUtilisee(reference));
		return reference;
	}

	/** Petite poche de vérification d'unicité du repository, sans couplage direct. */
	@FunctionalInterface
	public interface UniciteReferenceVerificatrice {
		boolean dejaUtilisee(String reference);
	}
}
