package com.galactique.gestionecole.schoolclasses.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;
import com.galactique.gestionecole.positions.domain.PositionRepository;
import com.galactique.gestionecole.schoolclasses.domain.AcademicYearRepository;
import com.galactique.gestionecole.security.domain.Permission;
import com.galactique.gestionecole.security.domain.Role;
import com.galactique.gestionecole.security.domain.SecurityAccountProvisioner;
import com.galactique.gestionecole.security.domain.SecurityDirectoryRepository;
import com.galactique.gestionecole.security.infrastructure.jwt.JwtTokenService;
import com.galactique.gestionecole.support.LicensedEstablishmentFixture;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class AcademicYearVisibilityControllerTests {
	@Autowired private MockMvc mvc;
	@Autowired private EstablishmentRepository schools;
	@Autowired private AcademicYearRepository years;
	@Autowired private EmployeeRepository employees;
	@Autowired private PositionRepository positions;
	@Autowired private SecurityAccountProvisioner accounts;
	@Autowired private SecurityDirectoryRepository directory;
	@Autowired private JdbcTemplate jdbc;
	@Autowired private EntityManager entityManager;
	@Autowired private LicenseApplicationService licenses;

	@Test
	void aSingleDirectPermissionListsActiveYearsInDescendingOrderWithoutALicense() throws Exception {
		var school = school();
		String role = localRole(school.id());
		var employee = member(school.id(), Set.of(role), Set.of(), null);
		var current = year(school.id(), 2026);
		var next = year(school.id(), 2027);
		var archived = year(school.id(), 2025);
		years.disable(archived, school.id());
		var foreign = school();
		year(foreign.id(), 2028);
		mvc.perform(get(url(school.id())).with(tokenFor(employee.email())))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[*].id", contains(next.toString(), current.toString())))
			.andExpect(jsonPath("$[0].establishmentId").value(school.id().toString()));
		mvc.perform(get(url(foreign.id())).with(tokenFor(employee.email())))
			.andExpect(status().isForbidden());
	}

	@Test
	void appliesDirectAndInheritedPermissionsSeparatelyForEachMembership() throws Exception {
		var first = school();
		var second = school();
		var third = school();
		String firstRole = localRole(first.id());
		String secondRole = localRole(second.id());
		var employee = member(first.id(), Set.of(firstRole), Set.of(), null);
		member(second.id(), Set.of(), Set.of(secondRole), employee);
		member(third.id(), Set.of(), Set.of(firstRole), employee);
		for (var school : java.util.List.of(first, second)) {
			var year = year(school.id(), 2026);
			mvc.perform(get(url(school.id())).with(tokenFor(employee.email())))
				.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(year.toString()));
		}
		mvc.perform(get(url(third.id())).with(tokenFor(employee.email())))
			.andExpect(status().isForbidden());
	}

	@Test
	void revokedPositionAndMembershipTakeEffectWithTheSameAuthentication() throws Exception {
		var school = school();
		String role = localRole(school.id());
		var employee = member(school.id(), Set.of(), Set.of(role), null);
		mvc.perform(get(url(school.id())).with(tokenFor(employee.email())))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
		positions.disable(employee.positionId(), school.id());
		mvc.perform(get(url(school.id())).with(tokenFor(employee.email())))
			.andExpect(status().isForbidden());
		employees.updateRoles(employee.id(), Set.of(role));
		mvc.perform(get(url(school.id())).with(tokenFor(employee.email())))
			.andExpect(status().isOk());
		employees.disable(employee.id());
		mvc.perform(get(url(school.id())).with(tokenFor(employee.email())))
			.andExpect(status().isForbidden());
	}

	@Test
	void rejectsDisabledRolesLockedAccountsAndDisabledEstablishments() throws Exception {
		var school = school();
		String role = localRole(school.id());
		var employee = member(school.id(), Set.of(role), Set.of(), null);
		entityManager.flush();
		jdbc.update("UPDATE security_roles SET enabled = FALSE WHERE code = ?", role);
		entityManager.clear();
		mvc.perform(get(url(school.id())).with(tokenFor(employee.email())))
			.andExpect(status().isForbidden());
		jdbc.update("UPDATE security_roles SET enabled = TRUE WHERE code = ?", role);
		jdbc.update("UPDATE security_user_accounts SET locked = TRUE WHERE id = ?", employee.userAccountId());
		entityManager.clear();
		mvc.perform(get(url(school.id())).with(tokenFor(employee.email())))
			.andExpect(status().isUnauthorized());
		jdbc.update("UPDATE security_user_accounts SET locked = FALSE WHERE id = ?", employee.userAccountId());
		entityManager.clear();
		schools.disable(school.id());
		mvc.perform(get(url(school.id())).with(tokenFor(employee.email())))
			.andExpect(status().isForbidden());
	}

	@Test
	void platformReaderListsYearsWithoutSpecificAcademicPermissionOrLicense() throws Exception {
		var school = school();
		var year = year(school.id(), 2026);
		mvc.perform(get(url(school.id())).with(tokenFor("admin-tech")))
			.andExpect(status().isForbidden());
	}

	@Test
	void listAccessDoesNotGrantCalendarManagementRights() throws Exception {
		var school = school();
		String role = localRole(school.id());
		var employee = member(school.id(), Set.of(role), Set.of(), null);
		var year = year(school.id(), 2026);
		String url = url(school.id());
		mvc.perform(post(url).with(tokenFor(employee.email())).contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"2028-2029\",\"startsOn\":\"2028-09-01\",\"endsOn\":\"2029-06-30\"}"))
			.andExpect(status().isForbidden());
		mvc.perform(patch(url + "/" + year).with(tokenFor(employee.email()))
			.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Changed\"}"))
			.andExpect(status().isForbidden());
		mvc.perform(delete(url + "/" + year).with(tokenFor(employee.email())))
			.andExpect(status().isForbidden());
		mvc.perform(get(url + "/" + year + "/periods").with(tokenFor(employee.email())))
			.andExpect(status().isForbidden());
		mvc.perform(get(url)).andExpect(status().isUnauthorized());
		mvc.perform(get(url).with(tokenFor("unknown@example.org"))).andExpect(status().isUnauthorized());
		mvc.perform(get(url(UUID.randomUUID())).with(tokenFor(employee.email())))
			.andExpect(status().isForbidden());
	}

	@Autowired private JwtTokenService tokens;
	private org.springframework.test.web.servlet.request.RequestPostProcessor tokenFor(String username) {
		String token = tokens.issue(org.springframework.security.authentication.UsernamePasswordAuthenticationToken
			.authenticated(username, null, java.util.List.of())).value();
		return request -> { request.addHeader("Authorization", "Bearer " + token); return request; };
	}

	private Establishment school() {
		return LicensedEstablishmentFixture.create(schools, licenses, "YRS-" + UUID.randomUUID(), "School years visibility");
	}

	private UUID year(UUID schoolId, int startYear) {
		return years.save(schoolId, startYear + "-" + (startYear + 1),
			LocalDate.of(startYear, 9, 1), LocalDate.of(startYear + 1, 6, 30)).id();
	}

	private String localRole(UUID schoolId) {
		String code = "LOCAL_" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
		directory.createRole(new Role(code, "Local role", null, true,
			Set.of(new Permission("can_academic_year_read", "Read school years")), schoolId));
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
			employees.findAllByUserAccountId(accountId).forEach(member -> roles.addAll(member.roleCodes()));
			accounts.updateEmployeeRoles(accountId, roles);
		}
		return employees.attachUserAccount(employee.id(), accountId);
	}

	private String url(UUID schoolId) {
		return "/api/v1/establishments/" + schoolId + "/school-years";
	}
}
