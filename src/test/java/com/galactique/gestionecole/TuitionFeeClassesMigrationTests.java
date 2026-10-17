package com.galactique.gestionecole;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.DriverManager;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class TuitionFeeClassesMigrationTests {
	@Test
	void preservesExistingSingleClassFeesAndGlobalScope() throws Exception {
		String url = "jdbc:h2:mem:tuition_classes_upgrade_" + UUID.randomUUID()
			+ ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";
		try (var anchor = DriverManager.getConnection(url, "sa", "")) {
			Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration")
				.target("3").load().migrate();
			JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(url, "sa", ""));
			UUID schoolId = UUID.randomUUID();
			UUID yearId = UUID.randomUUID();
			UUID classId = UUID.randomUUID();
			UUID classFeeId = UUID.randomUUID();
			UUID globalFeeId = UUID.randomUUID();
			jdbc.update("""
				INSERT INTO establishments (id, code, name, created_at, updated_at)
				VALUES (?, 'UPGRADE', 'Upgrade School', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", schoolId);
			jdbc.update("""
				INSERT INTO academic_years (id, establishment_id, name, starts_on, ends_on, created_at, updated_at)
				VALUES (?, ?, '2025-2026', DATE '2025-09-01', DATE '2026-06-30', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", yearId, schoolId);
			jdbc.update("""
				INSERT INTO school_classes (id, establishment_id, academic_year_id, code, name, created_at, updated_at)
				VALUES (?, ?, ?, 'A', 'Class A', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				""", classId, schoolId, yearId);
			for (UUID feeId : java.util.List.of(classFeeId, globalFeeId)) {
				jdbc.update("""
					INSERT INTO tuition_fee_definitions
					(id, establishment_id, academic_year_id, name, amount, currency, due_date, school_class_id, created_at)
					VALUES (?, ?, ?, ?, 100.00, 'USD', DATE '2026-01-31', ?, CURRENT_TIMESTAMP)
					""", feeId, schoolId, yearId, feeId.toString(), feeId.equals(classFeeId) ? classId : null);
			}
			Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration").load().migrate();
			assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM tuition_fee_classes", Integer.class));
			assertEquals(classId, jdbc.queryForObject(
				"SELECT school_class_id FROM tuition_fee_classes WHERE fee_id = ? AND establishment_id = ? AND academic_year_id = ?",
				UUID.class, classFeeId, schoolId, yearId));
			assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM tuition_fee_classes WHERE fee_id = ?",
				Integer.class, globalFeeId));
			assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM tuition_fee_definitions WHERE amount = 100.00",
				Integer.class));
		}
	}
}
