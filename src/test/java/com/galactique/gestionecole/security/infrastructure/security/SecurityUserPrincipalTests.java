package com.galactique.gestionecole.security.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.galactique.gestionecole.security.domain.Permission;
import com.galactique.gestionecole.security.domain.Role;
import com.galactique.gestionecole.security.domain.UserAccount;

class SecurityUserPrincipalTests {
	@Test
	void exposesEnabledRoleAndPermissionAuthorities() {
		Permission permission = new Permission("STUDENT_READ", "Read student records");
		Role role = new Role("TEACHER", "Teacher", null, true, Set.of(permission));
		SecurityUserPrincipal principal = new SecurityUserPrincipal(createAccount(true, false, Set.of(role)));

		assertTrue(principal.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_TEACHER")));
		assertTrue(principal.getAuthorities().contains(new SimpleGrantedAuthority("STUDENT_READ")));
	}

	@Test
	void exposesConventionPermissionAuthorities() {
		Permission permission = new Permission("can_position_read", "Read positions");
		Role role = new Role("ESTABLISHMENT_ADMIN", "Establishment administrator", null, true, Set.of(permission));
		SecurityUserPrincipal principal = new SecurityUserPrincipal(createAccount(true, false, Set.of(role)));

		assertTrue(principal.getAuthorities().contains(new SimpleGrantedAuthority("can_position_read")));
	}

	@Test
	void doesNotGrantAuthoritiesFromDisabledRoles() {
		Permission permission = new Permission("STUDENT_READ", "Read student records");
		Role role = new Role("TEACHER", "Teacher", null, false, Set.of(permission));
		SecurityUserPrincipal principal = new SecurityUserPrincipal(createAccount(true, false, Set.of(role)));

		assertTrue(principal.getAuthorities().isEmpty());
	}

	@Test
	void reflectsAccountEnabledAndLockedStatus() {
		Role role = new Role(
			"TEACHER",
			"Teacher",
			null,
			true,
			Set.of(new Permission("STUDENT_READ", "Read student records")));
		SecurityUserPrincipal principal = new SecurityUserPrincipal(createAccount(false, true, Set.of(role)));

		assertFalse(principal.isEnabled());
		assertFalse(principal.isAccountNonLocked());
	}

	private UserAccount createAccount(boolean enabled, boolean locked, Set<Role> roles) {
		Instant now = Instant.now();
		return new UserAccount(
			UUID.randomUUID(),
			"teacher@example.org",
			"$2a$10$hashedPasswordValue",
			enabled,
			locked,
			now,
			now,
			null,
			roles);
	}
}