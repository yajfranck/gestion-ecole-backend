package com.galactique.gestionecole.security.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record UserAccount(
	UUID id,
	String username,
	String passwordHash,
	boolean enabled,
	boolean locked,
	Instant createdAt,
	Instant updatedAt,
	Instant lastLoginAt,
	Set<Role> roles) {
	public UserAccount {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(username, "username must not be null");
		username = username.strip().toLowerCase(Locale.ROOT);
		if (username.isBlank() || username.length() > 100) {
			throw new IllegalArgumentException("Username must contain between 1 and 100 characters");
		}
		Objects.requireNonNull(passwordHash, "passwordHash must not be null");
		if (passwordHash.isBlank() || passwordHash.length() > 255) {
			throw new IllegalArgumentException("Password hash must contain between 1 and 255 characters");
		}
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		Objects.requireNonNull(roles, "roles must not be null");
		if (roles.isEmpty()) {
			throw new IllegalArgumentException("A user account must have at least one role");
		}
		roles = Set.copyOf(roles);
	}
}