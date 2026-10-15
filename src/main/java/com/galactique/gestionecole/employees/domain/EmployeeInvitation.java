package com.galactique.gestionecole.employees.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record EmployeeInvitation(
	UUID id,
	UUID employeeId,
	String tokenHash,
	Instant createdAt,
	Instant expiresAt,
	Instant acceptedAt) {
	public EmployeeInvitation {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(employeeId, "employeeId must not be null");
		Objects.requireNonNull(tokenHash, "tokenHash must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(expiresAt, "expiresAt must not be null");
	}

	public boolean isAccepted() {
		return acceptedAt != null;
	}
}