package com.akrouty.gestiondemandes.request.application;

/** Projection non modifiable d'un client, matérialisée dans la transaction du cas d'utilisation. */
public record ClientConsultation(Long id, String nom, String email, String telephone) {
}
