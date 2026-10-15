package com.galactique.gestionecole.guardians.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.galactique.gestionecole.guardians.domain.GuardianInvitation;

@Entity
@Table(name = "guardian_invitations")
public class GuardianInvitationEntity {
	@Id
	private UUID id;
	@Column(nullable = false)
	private UUID guardianId;
	@Column(nullable = false, unique = true, length = 64)
	private String tokenHash;
	@Column(nullable = false, updatable = false)
	private Instant createdAt;
	@Column(nullable = false)
	private Instant expiresAt;
	@Column
	private Instant acceptedAt;

	protected GuardianInvitationEntity() {
	}

	public GuardianInvitationEntity(GuardianInvitation invitation) {
		id = invitation.id();
		guardianId = invitation.guardianId();
		tokenHash = invitation.tokenHash();
		createdAt = invitation.createdAt();
		expiresAt = invitation.expiresAt();
		acceptedAt = invitation.acceptedAt();
	}

	public GuardianInvitation toDomain() {
		return new GuardianInvitation(id, guardianId, tokenHash, createdAt, expiresAt, acceptedAt);
	}
	public void markAccepted(Instant value) { acceptedAt = value; }
}