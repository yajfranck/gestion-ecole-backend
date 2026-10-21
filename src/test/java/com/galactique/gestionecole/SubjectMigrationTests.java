package com.galactique.gestionecole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.DriverManager;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class SubjectMigrationTests {
	@Test
	void preservesExistingSubjectsAndAssignmentsAndAllowsNewSubjectsWithoutCodes() throws Exception {
		String url = "jdbc:h2:mem:subject_upgrade_" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";
		try (var anchor = DriverManager.getConnection(url, "sa", "")) {
			Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration").target("16").load().migrate();
			var jdbc = new JdbcTemplate(new DriverManagerDataSource(url, "sa", ""));
			UUID school = UUID.randomUUID(), year = UUID.randomUUID(), schoolClass = UUID.randomUUID();
			UUID subject = UUID.randomUUID(), assignment = UUID.randomUUID();
			jdbc.update("""
				INSERT INTO establishments (id, code, name, created_at, updated_at)
				VALUES (?, 'SUB-UPGRADE', 'Subject migration school', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", school);
			jdbc.update("""
				INSERT INTO academic_years (id, establishment_id, name, starts_on, ends_on, created_at, updated_at)
				VALUES (?, ?, '2026-2027', DATE '2026-09-01', DATE '2027-06-30', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", year, school);
			jdbc.update("""
				INSERT INTO school_classes (id, establishment_id, academic_year_id, code, name, created_at, updated_at)
				VALUES (?, ?, ?, 'CLASS', 'Existing class', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", schoolClass, school, year);
			jdbc.update("""
				INSERT INTO subjects (id, establishment_id, code, name, created_at, updated_at)
				VALUES (?, ?, 'MATH', 'Mathématiques', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", subject, school);
			jdbc.update("""
				INSERT INTO school_class_subjects (id, establishment_id, school_class_id, subject_id, created_at, updated_at)
				VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", assignment, school, schoolClass, subject);
			Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration").load().migrate();
			assertEquals("Mathématiques", jdbc.queryForObject("SELECT name FROM subjects WHERE id = ?", String.class, subject));
			assertEquals("MATH", jdbc.queryForObject("SELECT code FROM subjects WHERE id = ?", String.class, subject));
			assertEquals(subject, jdbc.queryForObject("SELECT subject_id FROM school_class_subjects WHERE id = ?", UUID.class, assignment));
			UUID newSubject = UUID.randomUUID();
			jdbc.update("""
				INSERT INTO subjects (id, establishment_id, name, created_at, updated_at)
				VALUES (?, ?, 'Français', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", newSubject, school);
			assertNull(jdbc.queryForObject("SELECT code FROM subjects WHERE id = ?", String.class, newSubject));
		}
	}
}
