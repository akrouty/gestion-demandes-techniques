package com.akrouty.gestiondemandes.request.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.Objects;

/**
 * Client associé aux demandes (contexte {@code request}, module non autonome).
 *
 * <p>{@code email} est obligatoire mais NON unique : aucune règle
 * d'unicité validée ne le justifie (ADR-003).</p>
 */
@Entity
@Table(name = "client")
@SequenceGenerator(name = "client_seq", sequenceName = "client_seq", allocationSize = 1)
public class Client {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "client_seq")
	private Long id;

	@Column(nullable = false)
	private String nom;

	@Column(nullable = false)
	private String email;

	@Column(nullable = false)
	private String telephone;

	protected Client() {
	}

	public Client(String nom, String email, String telephone) {
		this.nom = Objects.requireNonNull(nom, "nom obligatoire");
		this.email = Objects.requireNonNull(email, "email obligatoire");
		this.telephone = Objects.requireNonNull(telephone, "telephone obligatoire");
	}

	public Long getId() {
		return id;
	}

	public String getNom() {
		return nom;
	}

	public String getEmail() {
		return email;
	}

	public String getTelephone() {
		return telephone;
	}
}
