package com.galactique.gestionecole;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.galactique.gestionecole.security.domain.UserAccountRepository;
import com.galactique.gestionecole.security.infrastructure.persistence.RoleJpaRepository;

@SpringBootTest(properties = {
	"spring.datasource.url=jdbc:h2:mem:production_flyway_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
	"spring.datasource.username=sa",
	"spring.datasource.password=",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.jpa.hibernate.ddl-auto=validate",
	"spring.flyway.locations=classpath:db/migration",
	"security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@ActiveProfiles("production")
class ProductionFlywayTests {
	@Autowired
	private org.flywaydb.core.Flyway flyway;

	@Test
	void usesSingleInitialMigration() {
		assertEquals(1, flyway.info().applied().length);
		assertEquals("1", flyway.info().current().getVersion().toString());
	}

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private RoleJpaRepository roleJpaRepository;

	@Test
	void productionMigrationsDoNotCreateDefaultAdminAccount() {
		assertTrue(userAccountRepository.findByUsername("admin-tech").isEmpty());
	}

	@Test
	void adminRoleCanOnboardAndReadEstablishmentsOnly() {
		var adminRole = roleJpaRepository.findByCode("ADMIN").orElseThrow();
		assertEquals(java.util.Set.of("can_establishment_onboard", "can_establishment_read"),
			adminRole.getPermissions().stream().map(permission -> permission.getCode())
				.collect(java.util.stream.Collectors.toSet()));
	}

	@Test
	void establishmentAdminHasRoleAndPermissionManagementPermissions() {
		var role = roleJpaRepository.findByCode("ESTABLISHMENT_ADMIN").orElseThrow();
		var permissionCodes = role.getPermissions().stream().map(permission -> permission.getCode())
			.collect(java.util.stream.Collectors.toSet());
		assertTrue(permissionCodes.containsAll(java.util.Set.of(
			"can_role_read", "can_role_create", "can_role_update", "can_permission_read")));
	}

	@Test
	void newEstablishmentAndAttendancePermissionsAreScopedToEstablishmentAdmin() {
		var establishmentAdmin = roleJpaRepository.findByCode("ESTABLISHMENT_ADMIN").orElseThrow();
		var establishmentAdminPermissions = establishmentAdmin.getPermissions().stream()
			.map(permission -> permission.getCode()).collect(java.util.stream.Collectors.toSet());
		assertTrue(establishmentAdminPermissions.containsAll(java.util.Set.of(
			"can_establishment_update", "can_attendance_read", "can_attendance_write")));

		var admin = roleJpaRepository.findByCode("ADMIN").orElseThrow();
		var adminPermissions = admin.getPermissions().stream()
			.map(permission -> permission.getCode()).collect(java.util.stream.Collectors.toSet());
		assertFalse(adminPermissions.contains("can_establishment_update"));
		assertFalse(adminPermissions.contains("can_attendance_read"));
		assertFalse(adminPermissions.contains("can_attendance_write"));
	}
}