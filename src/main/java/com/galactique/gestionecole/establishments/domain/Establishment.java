package com.galactique.gestionecole.establishments.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record Establishment(UUID id, String code, String name, boolean enabled, Instant createdAt, Instant updatedAt) {
	public Establishment {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(code, "code must not be null");
		code = code.strip().toUpperCase(Locale.ROOT);
		if (code.isBlank() || code.length() > 40) {
			throw new IllegalArgumentException("Code must contain between 1 and 40 characters");
		}
		Objects.requireNonNull(name, "name must not be null");
		name = name.strip();
		if (name.isBlank() || name.length() > 200) {
			throw new IllegalArgumentException("Name must contain between 1 and 200 characters");
		}
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}
}