package com.galactique.gestionecole.guardians.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import com.galactique.gestionecole.guardians.domain.Guardian;

@Entity
@Table(name = "guardians")
public class GuardianEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	@Column(nullable = false, length = 100)
	private String firstName;
	@Column(nullable = false, length = 100)
	private String lastName;
	@Column(nullable = false, unique = true, length = 254)
	private String email;
	@Column(unique = true)
	private UUID userAccountId;
	@Column(nullable = false)
	private boolean enabled = true;
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected GuardianEntity() {
	}

	public GuardianEntity(String firstName, String lastName, String email) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
	}

	@PrePersist
	void onCreate() { createdAt = Instant.now(); }

	public UUID getId() { return id; }
	public String getFirstName() { return firstName; }
	public String getLastName() { return lastName; }
	public String getEmail() { return email; }
	public UUID getUserAccountId() { return userAccountId; }
	public boolean isEnabled() { return enabled; }
	public Instant getCreatedAt() { return createdAt; }
	public void attachUserAccount(UUID accountId) { userAccountId = accountId; }
	public Guardian toDomain() {
		return new Guardian(id, firstName, lastName, email, userAccountId, enabled, createdAt);
	}
}