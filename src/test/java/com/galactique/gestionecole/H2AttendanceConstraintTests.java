package com.galactique.gestionecole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class H2AttendanceConstraintTests {
	@Test
	void validatesAttendanceStatusAfterTheSchemaConnectionIsClosed() throws SQLException {
		String url = "jdbc:h2:mem:attendance_constraint_" + UUID.randomUUID()
			+ ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
		try (var connection = DriverManager.getConnection(url, "sa", "");
			 var statement = connection.createStatement()) {
			statement.execute("""
				CREATE TABLE attendance_probe (
					id INTEGER PRIMARY KEY,
					status VARCHAR(20) NOT NULL,
					CONSTRAINT ck_attendance_status CHECK (status IN ('PRESENT', 'ABSENT'))
				)
				""");
			statement.executeUpdate("INSERT INTO attendance_probe VALUES (1, 'PRESENT')");
		}
		try (var connection = DriverManager.getConnection(url, "sa", "");
			 var statement = connection.createStatement()) {
			statement.execute("SET DB_CLOSE_DELAY 0");
			assertEquals(1, statement.executeUpdate("INSERT INTO attendance_probe VALUES (2, 'PRESENT')"));
			assertEquals(1, statement.executeUpdate("INSERT INTO attendance_probe VALUES (3, 'ABSENT')"));
			SQLException exception = assertThrows(SQLException.class,
				() -> statement.executeUpdate("INSERT INTO attendance_probe VALUES (4, 'INVALID')"));
			assertEquals("23513", exception.getSQLState());
			try (var result = statement.executeQuery("SELECT COUNT(*) FROM attendance_probe")) {
				assertTrue(result.next());
				assertEquals(3, result.getInt(1));
			}
		}
	}
}
