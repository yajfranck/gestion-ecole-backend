package com.galactique.gestionecole.security.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "security_refresh_tokens")
public class RefreshTokenEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true, length = 64)
	private String tokenHash;

	@Column(nullable = false)
	private UUID userAccountId;

	@Column(nullable = false)
	private UUID familyId;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant expiresAt;

	@Column
	private Instant revokedAt;

	protected RefreshTokenEntity() {
	}

	public RefreshTokenEntity(
		String tokenHash,
		UUID userAccountId,
		UUID familyId,
		Instant createdAt,
		Instant expiresAt) {
		this.tokenHash = tokenHash;
		this.userAccountId = userAccountId;
		this.familyId = familyId;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
	}

	public UUID getId() {
		return id;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public UUID getUserAccountId() {
		return userAccountId;
	}

	public UUID getFamilyId() {
		return familyId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

	public boolean isRevoked() {
		return revokedAt != null;
	}

	public void revoke(Instant revokedAt) {
		this.revokedAt = revokedAt;
	}
}