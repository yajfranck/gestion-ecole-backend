package com.galactique.gestionecole.students.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record Student(
	UUID id,
	UUID establishmentId,
	String studentNumber,
	String firstName,
	String lastName,
	java.time.LocalDate dateOfBirth,
	boolean enabled,
	Instant createdAt,
	Instant updatedAt) {
	public Student(UUID id, UUID establishmentId, String studentNumber, String firstName, String lastName,
		boolean enabled, Instant createdAt, Instant updatedAt) {
		this(id, establishmentId, studentNumber, firstName, lastName, null, enabled, createdAt, updatedAt);
	}
	public Student {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(establishmentId, "establishmentId must not be null");
		studentNumber = normalizeStudentNumber(studentNumber);
		firstName = required(firstName, "firstName", 100);
		lastName = required(lastName, "lastName", 100);
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}

	public static String normalizeStudentNumber(String studentNumber) {
		return required(studentNumber, "studentNumber", 40).toUpperCase(Locale.ROOT);
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
