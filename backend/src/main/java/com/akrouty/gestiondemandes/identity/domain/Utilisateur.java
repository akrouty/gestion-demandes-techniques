package com.akrouty.gestiondemandes.identity.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Utilisateur interne persisté (module {@code identity}).
 *
 * <p>{@code email} est obligatoire, unique et normalisé avant persistance.
 * {@code passwordHash} est une donnée technique de sécurité : aucun mot de
 * passe en clair n'est jamais persisté. La création et le hachage réel du mot
 * de passe sont traités dans un bloc suivant.</p>
 */
@Entity
@Table(
		name = "utilisateur",
		uniqueConstraints = @UniqueConstraint(name = "uk_utilisateur_email", columnNames = "email")
)
@SequenceGenerator(name = "utilisateur_seq", sequenceName = "utilisateur_seq", allocationSize = 1)
public class Utilisateur {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "utilisateur_seq")
	private Long id;

	@Column(nullable = false)
	private String nom;

	@Column(nullable = false)
	private String email;

	@Column(nullable = false)
	private boolean actif;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(
			name = "utilisateur_role",
			joinColumns = @JoinColumn(name = "utilisateur_id"),
			uniqueConstraints = @UniqueConstraint(
					name = "uk_utilisateur_role",
					columnNames = {"utilisateur_id", "role"}
			)
	)
	@Column(name = "role", nullable = false)
	@Enumerated(EnumType.STRING)
	private Set<Role> roles = new LinkedHashSet<>();

	protected Utilisateur() {
	}

	public Utilisateur(String nom, String email, boolean actif, String passwordHash, Set<Role> roles) {
		this.nom = Objects.requireNonNull(nom, "nom obligatoire");
		this.email = EmailNormalizer.normalize(Objects.requireNonNull(email, "email obligatoire"));
		this.actif = actif;
		this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash obligatoire");
		this.roles = new LinkedHashSet<>(Objects.requireNonNull(roles, "roles obligatoires"));
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

	public boolean isActif() {
		return actif;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public Set<Role> getRoles() {
		return roles;
	}
}
