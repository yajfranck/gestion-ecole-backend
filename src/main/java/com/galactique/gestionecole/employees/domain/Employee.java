package com.galactique.gestionecole.employees.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record Employee(
	UUID id,
	String employeeNumber,
	String firstName,
	String lastName,
	String email,
	UUID positionId,
	String positionName,
	boolean enabled,
	Set<String> directRoleCodes,
	Set<String> roleCodes,
	UUID establishmentId,
	UUID userAccountId,
	Instant createdAt,
	Instant updatedAt) {
	public Employee {
		Objects.requireNonNull(id, "id must not be null");
		employeeNumber = required(employeeNumber, "employeeNumber", 40);
		firstName = required(firstName, "firstName", 100);
		lastName = required(lastName, "lastName", 100);
		email = required(email, "email", 254).toLowerCase(Locale.ROOT);
		Objects.requireNonNull(positionId, "positionId must not be null");
		positionName = required(positionName, "positionName", 100);
		Objects.requireNonNull(directRoleCodes, "directRoleCodes must not be null");
		directRoleCodes = Set.copyOf(directRoleCodes);
		Objects.requireNonNull(roleCodes, "roleCodes must not be null");
		roleCodes = Set.copyOf(roleCodes);
		Objects.requireNonNull(establishmentId, "establishmentId must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}

	public boolean establishmentAdmin() {
		return roleCodes.contains("ESTABLISHMENT_ADMIN");
	}

	private static String required(String value, String field, int maxLength) {
		Objects.requireNonNull(value, field + " must not be null");
		String normalized = value.strip();
		if (normalized.isBlank() || normalized.length() > maxLength) {
			throw new IllegalArgumentException(field + " must contain between 1 and " + maxLength + " characters");
		}
		return normalized;
	}
}