package com.galactique.gestionecole;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.DriverManager;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class ClassroomMigrationTests {
	@Test
	void preservesExistingEnrollmentsAndEnforcesOneClassroomInTheSameClassScope() throws Exception {
		String url = "jdbc:h2:mem:classroom_upgrade_" + UUID.randomUUID()
			+ ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";
		try (var anchor = DriverManager.getConnection(url, "sa", "")) {
			Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration").target("6").load().migrate();
			JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(url, "sa", ""));
			UUID school = UUID.randomUUID(), year = UUID.randomUUID(), student = UUID.randomUUID();
			UUID firstClass = UUID.randomUUID(), archivedClass = UUID.randomUUID(), enrollment = UUID.randomUUID();
			jdbc.update("""
				INSERT INTO establishments (id, code, name, created_at, updated_at)
				VALUES (?, 'ROOM-UPGRADE', 'Migration School', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", school);
			jdbc.update("""
				INSERT INTO academic_years (id, establishment_id, name, starts_on, ends_on, created_at, updated_at)
				VALUES (?, ?, '2026-2027', DATE '2026-09-01', DATE '2027-06-30', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", year, school);
			for (UUID classId : java.util.List.of(firstClass, archivedClass)) {
				jdbc.update("""
					INSERT INTO school_classes (id, establishment_id, academic_year_id, code, name, enabled, created_at, updated_at)
					VALUES (?, ?, ?, ?, 'Sixième', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
					""", classId, school, year, classId.toString(), classId.equals(firstClass));
			}
			jdbc.update("""
				INSERT INTO students (id, establishment_id, student_number, first_name, last_name, created_at, updated_at)
				VALUES (?, ?, 'STU-UPGRADE', 'Existing', 'Student', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", student, school);
			jdbc.update("""
				INSERT INTO enrollments (id, establishment_id, student_id, academic_year_id, school_class_id, created_at)
				VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
				""", enrollment, school, student, year, firstClass);
			var createdAt = jdbc.queryForObject("SELECT created_at FROM enrollments WHERE id = ?",
				java.time.OffsetDateTime.class, enrollment);
			Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration").load().migrate();
			assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM classrooms", Integer.class));
			assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM classrooms WHERE max_students = 40", Integer.class));
			assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM classrooms WHERE code = 'DEFAULT' OR name = 'Salle principale'",
				Integer.class));
			assertEquals(false, jdbc.queryForObject("SELECT enabled FROM classrooms WHERE school_class_id = ?",
				Boolean.class, archivedClass));
			assertEquals(firstClass, jdbc.queryForObject("SELECT classroom_id FROM enrollments WHERE id = ?",
				UUID.class, enrollment));
			assertEquals(false, jdbc.queryForObject("SELECT advance_accepted FROM enrollments WHERE id = ?",
				Boolean.class, enrollment));
			assertNull(jdbc.queryForObject("SELECT payment_decision_by FROM enrollments WHERE id = ?", String.class, enrollment));
			assertEquals(createdAt, jdbc.queryForObject("SELECT created_at FROM enrollments WHERE id = ?",
				java.time.OffsetDateTime.class, enrollment));
			assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
				"UPDATE enrollments SET classroom_id = NULL WHERE id = ?", enrollment));
			assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
				"UPDATE enrollments SET classroom_id = ? WHERE id = ?", archivedClass, enrollment));
		}
	}
}
