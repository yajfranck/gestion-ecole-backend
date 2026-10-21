package com.galactique.gestionecole.security.authorization;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class AuthorizationMigrationTests {
	@Test
	void convertsLegacyAssignmentsOncePreservesOtherPermissionsAndNeverRestoresARevocation() {
		String url = "jdbc:h2:mem:auth_migration_" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
		var source = new DriverManagerDataSource(url, "sa", "");
		Flyway.configure().dataSource(source).locations("classpath:db/migration", "classpath:db/migration-local")
			.target("17").load().migrate();
		var jdbc = new JdbcTemplate(source);
		UUID roleId = UUID.randomUUID();
		jdbc.update("INSERT INTO security_roles(id, code, name, enabled) VALUES(?, 'SUPER_ADMIN', 'Explicit super administrator', TRUE)", roleId);
		jdbc.update("INSERT INTO security_role_permissions(role_id, permission_id) SELECT ?, id FROM security_permissions WHERE code IN ('ADMIN_ACCESS', 'can_student_read')", roleId);
		var migration = Flyway.configure().dataSource(source)
			.locations("classpath:db/migration", "classpath:db/migration-local").load();
		assertEquals(2, migration.migrate().migrationsExecuted);
		assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM security_permissions WHERE code IN ('ADMIN_ACCESS','ESTABLISHMENT_STAFF_MANAGEMENT','STAFF_ACCESS')", Integer.class));
		assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM security_role_permissions rp JOIN security_permissions p ON p.id=rp.permission_id WHERE rp.role_id=? AND p.code='can_student_read'", Integer.class, roleId));
		assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM security_role_scopes WHERE role_id=? AND scope_code='PLATFORM'", Integer.class, roleId));
		assertEquals(1, jdbc.update("DELETE FROM security_role_permissions WHERE role_id=? AND permission_id=(SELECT id FROM security_permissions WHERE code='can_employee_update')", roleId));
		int assignments = jdbc.queryForObject("SELECT COUNT(*) FROM security_role_permissions", Integer.class);
		assertEquals(0, migration.migrate().migrationsExecuted);
		assertEquals(assignments, jdbc.queryForObject("SELECT COUNT(*) FROM security_role_permissions", Integer.class));
		assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM security_role_permissions rp JOIN security_permissions p ON p.id=rp.permission_id WHERE rp.role_id=? AND p.code='can_employee_update'", Integer.class, roleId));
		migration.validate();
	}
}
