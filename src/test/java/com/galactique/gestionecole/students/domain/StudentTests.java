package com.galactique.gestionecole.students.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class StudentTests {
	@Test
	void normalizesStudentNumberAndNames() {
		Instant now = Instant.now();
		Student student = new Student(UUID.randomUUID(), UUID.randomUUID(), " stu-001 ", " Ada ", " Lovelace ",
			true, now, now);

		assertEquals("STU-001", student.studentNumber());
		assertEquals("Ada", student.firstName());
		assertEquals("Lovelace", student.lastName());
	}

	@Test
	void rejectsBlankStudentNumber() {
		Instant now = Instant.now();
		UUID studentId = UUID.randomUUID();
		UUID establishmentId = UUID.randomUUID();

		assertThrows(IllegalArgumentException.class, () -> new Student(
			studentId, establishmentId, "  ", "Ada", "Lovelace", true, now, now));
	}
}