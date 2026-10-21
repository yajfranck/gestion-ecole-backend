package com.galactique.gestionecole.establishments.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.employees.domain.Employee;
import com.galactique.gestionecole.employees.domain.EmployeeRepository;
import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.guardians.domain.GuardianRepository;
import com.galactique.gestionecole.positions.domain.PositionRepository;
import com.galactique.gestionecole.security.domain.Permission;
import com.galactique.gestionecole.security.domain.Role;
import com.galactique.gestionecole.security.domain.SecurityAccountProvisioner;
import com.galactique.gestionecole.security.domain.SecurityDirectoryRepository;
import com.galactique.gestionecole.security.infrastructure.jwt.JwtTokenService;
import com.galactique.gestionecole.students.domain.StudentRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class EstablishmentVisibilityControllerTests {
	@Autowired private MockMvc mvc;
	@Autowired private EstablishmentRepository schools;
	@Autowired private EmployeeRepository employees;
	@Autowired private PositionRepository positions;
	@Autowired private SecurityAccountProvisioner accounts;
	@Autowired private SecurityDirectoryRepository directory;
	@Autowired private GuardianRepository guardians;
	@Autowired private StudentRepository students;
	@Autowired private JdbcTemplate jdbc;
	@Autowired private EntityManager entityManager;

	@Test
	void staffWithOneDirectPermissionSeesOnlyTheirEstablishmentWithoutALicense() throws Exception {
		var school = school();
		school();
		String role = localRole(school.id(), "can_student_read");
		var employee = member(school.id(), Set.of(role), Set.of(), null);
		mvc.perform(get("/api/v1/establishments").with(tokenFor(employee.email())))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].id").value(school.id().toString()));
	}

	@Test
	void combinesDirectAndPositionPermissionsAcrossSeveralEstablishments() throws Exception {
		var first = school();
		var second = school();
		school();
		String firstRole = localRole(first.id(), "can_student_read");
		String secondRole = localRole(second.id(), "can_payment_record");
		var employee = member(first.id(), Set.of(firstRole), Set.of(), null);
		member(second.id(), Set.of(), Set.of(secondRole), employee);
		mvc.perform(get("/api/v1/establishments").with(tokenFor(employee.email())))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[*].id", hasItem(first.id().toString())))
			.andExpect(jsonPath("$[*].id", hasItem(second.id().toString())));
	}

	@Test
	void permissionFromAnotherEstablishmentDoesNotExposeAnUnentitledMembership() throws Exception {
		var first = school();
		var second = school();
		String role = localRole(first.id(), "can_student_read");
		var employee = member(first.id(), Set.of(role), Set.of(), null);
		// A foreign role code must never grant visibility in the second school.
		member(second.id(), Set.of(), Set.of(role), employee);
		mvc.perform(get("/api/v1/establishments").with(tokenFor(employee.email())))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
			;
	}

	@Test
	void filtersDisabledMembershipsAndEstablishments() throws Exception {
		var first = school();
		var second = school();
		var third = school();
		var employee = member(first.id(), Set.of("STAFF"), Set.of(), null);
		var inactive = member(second.id(), Set.of("STAFF"), Set.of(), employee);
		member(third.id(), Set.of("STAFF"), Set.of(), employee);
		employees.disable(inactive.id());
		schools.disable(third.id());
		mvc.perform(get("/api/v1/establishments").with(tokenFor(employee.email())))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].id").value(first.id().toString()));
	}

	@Test
	void disabledPositionDoesNotContributePermissionsButDirectRolesStillApply() throws Exception {
		var first = school();
		var second = school();
		var employee = member(first.id(), Set.of(), Set.of("STAFF"), null);
		var direct = member(second.id(), Set.of("STAFF"), Set.of("STAFF"), employee);
		positions.disable(employee.positionId(), first.id());
		positions.disable(direct.positionId(), second.id());
		mvc.perform(get("/api/v1/establishments").with(tokenFor(employee.email())))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
			;
	}

	@Test
	void disabledRoleIsRecheckedEvenWhenAuthenticationStillContainsItsPermissions() throws Exception {
		var first = school();
		var second = school();
		String role = localRole(first.id(), "can_student_read");
		var employee = member(first.id(), Set.of(role), Set.of(), null);
		member(second.id(), Set.of("STAFF"), Set.of(), employee);
		entityManager.flush();
		jdbc.update("UPDATE security_roles SET enabled = FALSE WHERE code = ?", role);
		entityManager.clear();
		mvc.perform(get("/api/v1/establishments").with(tokenFor(employee.email())))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
			;
	}

	@Test
	void inactiveOrLockedAccountsAndAccountsWithoutActivePermissionsAreForbidden() throws Exception {
		var school = school();
		String role = localRole(school.id(), "can_student_read");
		var employee = member(school.id(), Set.of(role), Set.of(), null);
		entityManager.flush();
		jdbc.update("UPDATE security_user_accounts SET locked = TRUE WHERE id = ?", employee.userAccountId());
		entityManager.clear();
		mvc.perform(get("/api/v1/establishments").with(tokenFor(employee.email())))
			.andExpect(status().isUnauthorized());
		jdbc.update("UPDATE security_user_accounts SET locked = FALSE, enabled = FALSE WHERE id = ?", employee.userAccountId());
		entityManager.clear();
		mvc.perform(get("/api/v1/establishments").with(tokenFor(employee.email())))
			.andExpect(status().isUnauthorized());
		jdbc.update("UPDATE security_user_accounts SET enabled = TRUE WHERE id = ?", employee.userAccountId());
		jdbc.update("UPDATE security_roles SET enabled = FALSE WHERE code = ?", role);
		entityManager.clear();
		mvc.perform(get("/api/v1/establishments").with(tokenFor(employee.email())))
			.andExpect(status().isForbidden());
	}

	@Test
	void parentSeesLinkedChildrensSchoolsWithoutDuplicatesAndLosesUnlinkedAccess() throws Exception {
		var first = school();
		var second = school();
		school();
		String email = UUID.randomUUID() + "@example.org";
		var guardian = guardians.save("Parent", "Example", email);
		guardians.attachUserAccount(guardian.id(), accounts.createGuardianAccount(email, "StrongPassword123!"));
		var child = students.save(first.id(), "Child", "One", null);
		var sibling = students.save(first.id(), "Child", "Two", null);
		var other = students.save(second.id(), "Child", "Three", null);
		guardians.link(guardian.id(), child.id());
		guardians.link(guardian.id(), sibling.id());
		guardians.link(guardian.id(), other.id());
		mvc.perform(get("/api/v1/establishments").with(tokenFor(email)))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
		guardians.unlink(guardian.id(), other.id());
		mvc.perform(get("/api/v1/establishments").with(tokenFor(email)))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].id").value(first.id().toString()));
		students.archive(child.id(), first.id());
		students.archive(sibling.id(), first.id());
		mvc.perform(get("/api/v1/establishments").with(tokenFor(email)))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void staffAndGuardianAccessAreCombinedWithoutDuplicates() throws Exception {
		var first = school();
		var second = school();
		var employee = member(first.id(), Set.of("STAFF"), Set.of(), null);
		accounts.addGuardianRole(employee.userAccountId());
		var guardian = guardians.save("Parent", "Staff", employee.email());
		guardians.attachUserAccount(guardian.id(), employee.userAccountId());
		for (var school : java.util.List.of(first, second)) {
			guardians.link(guardian.id(), students.save(school.id(), "Child", "Example", null).id());
		}
		mvc.perform(get("/api/v1/establishments").with(tokenFor(employee.email())))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
	}

	@Test
	void localPermissionsDoNotGrantModificationDeletionOrGlobalSecurityAccess() throws Exception {
		var school = school();
		var employee = member(school.id(), Set.of("STAFF"), Set.of(), null);
		mvc.perform(patch("/api/v1/establishments/" + school.id()).with(tokenFor(employee.email()))
			.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Changed\"}"))
			.andExpect(status().isForbidden());
		mvc.perform(delete("/api/v1/establishments/" + school.id()).with(tokenFor(employee.email())))
			.andExpect(status().isForbidden());
		mvc.perform(get("/api/v1/user").with(tokenFor(employee.email())))
			.andExpect(status().isForbidden());
		mvc.perform(get("/api/v1/establishments"))
			.andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/establishments").with(tokenFor("unknown@example.org")))
			.andExpect(status().isUnauthorized());
	}

	@Autowired private JwtTokenService tokens;
	private org.springframework.test.web.servlet.request.RequestPostProcessor tokenFor(String username) {
		String token = tokens.issue(org.springframework.security.authentication.UsernamePasswordAuthenticationToken
			.authenticated(username, null, java.util.List.of())).value();
		return request -> { request.addHeader("Authorization", "Bearer " + token); return request; };
	}

	private Establishment school() {
		return schools.save("VIS-" + UUID.randomUUID(), "Visibility school");
	}

	private String localRole(UUID schoolId, String permission) {
		String code = "LOCAL_" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
		directory.createRole(new Role(code, "Local role", null, true,
			Set.of(new Permission(permission, "Visibility permission"), new Permission("can_establishment_read", "Read memberships")), schoolId));
		return code;
	}

	private Employee member(UUID schoolId, Set<String> directRoles, Set<String> positionRoles, Employee existing) {
		String suffix = UUID.randomUUID().toString();
		String email = existing == null ? suffix + "@example.org" : existing.email();
		var position = positions.save(schoolId, "Position " + suffix, positionRoles);
		var employee = employees.save(suffix, "Staff", "Example", email, position.id(), directRoles);
		Set<String> roles = new java.util.HashSet<>(directRoles);
		roles.addAll(positionRoles);
		UUID accountId;
		if (existing == null) {
			accountId = accounts.createEmployeeAccount(email, "StrongPassword123!", roles);
		} else {
			accountId = existing.userAccountId();
			roles.addAll(existing.roleCodes());
			accounts.updateEmployeeRoles(accountId, roles);
		}
		return employees.attachUserAccount(employee.id(), accountId);
	}
}
