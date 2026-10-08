package com.akrouty.gestiondemandes.request.presentation;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * DTO de mise à jour du traitement (API-CONTRACT-V1 §4.1).
 *
 * <p>Les drapeaux « fourni » distinguent STRICTEMENT un champ absent de la
 * requête (valeur inchangée) d'un champ présent (dont {@code null} : refusé
 * en {@code 400}). Jackson appelle le setter aussi bien pour une valeur que
 * pour {@code null} explicite, mais JAMAIS pour une propriété absente — ce
 * mécanisme ne nécessite aucune dépendance externe.</p>
 *
 * <p>Au moins un des deux champs doit être fourni ({@code 400} sinon) et un
 * champ fourni doit être non null et non blanc.</p>
 */
public class TraitementDemandeRequest {

	private String descriptionTraitement;
	private boolean descriptionTraitementFournie;
	private String solution;
	private boolean solutionFournie;

	@JsonSetter
	public void setDescriptionTraitement(String descriptionTraitement) {
		this.descriptionTraitement = descriptionTraitement;
		this.descriptionTraitementFournie = true;
	}

	@JsonSetter
	public void setSolution(String solution) {
		this.solution = solution;
		this.solutionFournie = true;
	}

	public String getDescriptionTraitement() {
		return descriptionTraitement;
	}

	@JsonIgnore
	public boolean isDescriptionTraitementFournie() {
		return descriptionTraitementFournie;
	}

	public String getSolution() {
		return solution;
	}

	@JsonIgnore
	public boolean isSolutionFournie() {
		return solutionFournie;
	}
}
