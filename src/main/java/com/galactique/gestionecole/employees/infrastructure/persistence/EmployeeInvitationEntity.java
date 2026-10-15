package com.galactique.gestionecole.employees.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.galactique.gestionecole.employees.domain.EmployeeInvitation;

@Entity
@Table(name = "employee_invitations")
public class EmployeeInvitationEntity {
	@Id
	private UUID id;

	@Column(nullable = false)
	private UUID employeeId;

	@Column(nullable = false, unique = true, length = 64)
	private String tokenHash;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant expiresAt;

	@Column
	private Instant acceptedAt;

	protected EmployeeInvitationEntity() {
	}

	public EmployeeInvitationEntity(EmployeeInvitation invitation) {
		this.id = invitation.id();
		this.employeeId = invitation.employeeId();
		this.tokenHash = invitation.tokenHash();
		this.createdAt = invitation.createdAt();
		this.expiresAt = invitation.expiresAt();
		this.acceptedAt = invitation.acceptedAt();
	}

	public UUID getId() { return id; }
	public UUID getEmployeeId() { return employeeId; }
	public String getTokenHash() { return tokenHash; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getExpiresAt() { return expiresAt; }
	public Instant getAcceptedAt() { return acceptedAt; }

	public void markAccepted(Instant acceptedAt) {
		this.acceptedAt = acceptedAt;
	}
}