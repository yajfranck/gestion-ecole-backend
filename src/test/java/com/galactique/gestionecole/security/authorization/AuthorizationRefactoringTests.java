package com.galactique.gestionecole.security.authorization;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.galactique.gestionecole.employees.domain.EmployeeRepository;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;
import com.galactique.gestionecole.licenses.domain.LicenseFeature;
import com.galactique.gestionecole.positions.domain.PositionRepository;
import com.galactique.gestionecole.schoolclasses.domain.AcademicYearRepository;
import com.galactique.gestionecole.security.domain.Permission;
import com.galactique.gestionecole.security.domain.Role;
import com.galactique.gestionecole.security.domain.SecurityAccountProvisioner;
import com.galactique.gestionecole.security.domain.SecurityDirectoryRepository;
import com.galactique.gestionecole.security.infrastructure.jwt.JwtTokenService;
import com.galactique.gestionecole.students.domain.StudentRepository;
import com.galactique.gestionecole.support.LicensedEstablishmentFixture;
import com.galactique.gestionecole.tuition.domain.TuitionFeeDraft;
import com.galactique.gestionecole.tuition.domain.TuitionFeeTypeDraft;
import com.galactique.gestionecole.tuition.domain.TuitionRepository;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:authorization_refactor;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class AuthorizationRefactoringTests {
	@Autowired MockMvc mvc;
	@Autowired JdbcTemplate jdbc;
	@Autowired JwtTokenService tokens;
	@Autowired EstablishmentRepository schools;
	@Autowired AcademicYearRepository years;
	@Autowired EmployeeRepository employees;
	@Autowired PositionRepository positions;
	@Autowired SecurityAccountProvisioner accounts;
	@Autowired SecurityDirectoryRepository directory;
	@Autowired LicenseApplicationService licenses;
	@Autowired jakarta.persistence.EntityManager entityManager;
	@Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlers;

	@Test
	void everySensitiveEndpointDeclaresItsOwnCataloguedPermission() {
		Set<String> publicMethods = Set.of("AuthenticationController#login", "AuthenticationController#refresh",
			"EmployeeController#acceptInvitation", "GuardianController#acceptInvitation");
		Set<String> catalog = directory.findAllPermissions().stream().map(Permission::code)
			.collect(java.util.stream.Collectors.toSet());
		int inspected = 0;
		for (var handler : handlers.getHandlerMethods().values()) {
			if (!handler.getBeanType().getPackageName().startsWith("com.galactique.gestionecole")) { continue; }
			var annotation = handler.getMethod().getDeclaredAnnotation(PreAuthorize.class);
			if (publicMethods.contains(handler.getBeanType().getSimpleName() + "#" + handler.getMethod().getName())) { continue; }
			assertNotNull(annotation, handler.toString());
			assertFalse(annotation.value().contains("hasRole"), handler.toString());
			assertFalse(annotation.value().contains("ADMIN_ACCESS"), handler.toString());
			var matcher = Pattern.compile("hasAuthority\\('([^']+)'\\)").matcher(annotation.value());
			boolean found = false;
			while (matcher.find()) {
				found = true;
				assertTrue(matcher.group(1).matches("can_[a-z0-9]+(?:_[a-z0-9]+)+"));
				assertTrue(catalog.contains(matcher.group(1)), matcher.group(1));
			}
			assertTrue(found, handler.toString());
			inspected++;
		}
		assertTrue(inspected >= 110, "Audit must cover the complete REST API");
		assertTrue(catalog.stream().allMatch(code -> code.startsWith("can_")));
	}

	@Test
	void administratorRoleAndLegacyAuthorityNeverReplaceTheEndpointPermission() throws Exception {
		for (String endpoint : List.of("/api/v1/establishments/" + UUID.randomUUID() + "/employees", "/api/v1/user", "/api/v1/role",
			"/api/v1/permission", "/api/v1/admin/license-offers", "/api/v1/establishments")) {
			mvc.perform(get(endpoint).with(user("admin-tech").authorities(
				new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ADMIN_ACCESS"))))
				.andExpect(status().isForbidden());
		}
		mvc.perform(get("/api/v1/admin/license-offers").header(HttpHeaders.AUTHORIZATION, bearer("admin-tech")))
			.andExpect(status().isOk());
	}

	@Test
	void revocationTakesEffectForAnAlreadyIssuedJwtAndIsNotRestoredByRefreshOrLogin() throws Exception {
		String token = bearer("admin-tech");
		mvc.perform(get("/api/v1/admin/license-offers").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isOk());
		int before = jdbc.queryForObject("SELECT COUNT(*) FROM security_role_permissions", Integer.class);
		jdbc.update("DELETE FROM security_role_permissions WHERE role_id=(SELECT id FROM security_roles WHERE code='ADMIN') AND permission_id=(SELECT id FROM security_permissions WHERE code='can_license_offer_read')");
		entityManager.clear();
		mvc.perform(get("/api/v1/admin/license-offers").header(HttpHeaders.AUTHORIZATION, token))
			.andExpect(status().isForbidden());
		String login = mvc.perform(post("/api/v1/login").contentType(org.springframework.http.MediaType.APPLICATION_JSON)
			.content("{\"login\":\"admin-tech\",\"password\":\"admin\"}"))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		String accessToken = com.jayway.jsonpath.JsonPath.read(login, "$.accessToken");
		String refreshToken = com.jayway.jsonpath.JsonPath.read(login, "$.refreshToken");
		mvc.perform(get("/api/v1/admin/license-offers").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isForbidden());
		String refreshed = mvc.perform(post("/api/v1/refresh").contentType(org.springframework.http.MediaType.APPLICATION_JSON)
			.content("{\"refreshToken\":\"" + refreshToken + "\"}"))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		String renewedToken = com.jayway.jsonpath.JsonPath.read(refreshed, "$.accessToken");
		mvc.perform(get("/api/v1/admin/license-offers").header(HttpHeaders.AUTHORIZATION, "Bearer " + renewedToken))
			.andExpect(status().isForbidden());
		assertEquals(before - 1, jdbc.queryForObject("SELECT COUNT(*) FROM security_role_permissions", Integer.class));
	}

	@Test
	void localPermissionAllowsOnlyItsOwnEstablishmentAndSchoolYear() throws Exception {
		UUID school = school();
		UUID foreign = school();
		String email = member(school, Set.of("can_student_read", "can_school_class_read"));
		String token = bearer(email);
		UUID year = years.save(school, "2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)).id();
		UUID foreignYear = years.save(foreign, "2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)).id();
		mvc.perform(get("/api/v1/establishments/" + school + "/students").header(HttpHeaders.AUTHORIZATION, token))
			.andExpect(status().isOk());
		mvc.perform(get("/api/v1/establishments/" + foreign + "/students").header(HttpHeaders.AUTHORIZATION, token))
			.andExpect(status().isForbidden());
		mvc.perform(get("/api/v1/establishments/" + school + "/school-years/" + year + "/classes")
			.header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isOk());
		mvc.perform(get("/api/v1/establishments/" + school + "/school-years/" + foreignYear + "/classes")
			.header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isNotFound());
	}

	@Test
	void permissionFromOneMembershipDoesNotLeakIntoAnotherMembership() throws Exception {
		UUID first = school();
		UUID second = school();
		String email = member(first, Set.of("can_subject_read", "can_permission_read"));
		UUID accountId = jdbc.queryForObject("SELECT id FROM security_user_accounts WHERE username=?", UUID.class, email);
		var position = positions.save(second, "Other school staff", Set.of("STAFF"));
		var employee = employees.save("E-" + UUID.randomUUID(), "Other", "School", email, position.id(), Set.of());
		employees.attachUserAccount(employee.id(), accountId);
		mvc.perform(get("/api/v1/establishments/" + first + "/subjects").header(HttpHeaders.AUTHORIZATION, bearer(email)))
			.andExpect(status().isOk());
		mvc.perform(get("/api/v1/establishments/" + second + "/subjects").header(HttpHeaders.AUTHORIZATION, bearer(email)))
			.andExpect(status().isForbidden());
		accounts.addEmployeeRoles(accountId, Set.of("STAFF"));
		String token = bearer(email);
		mvc.perform(get("/api/v1/permission").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isOk());
		employees.disable(employees.findByUserAccountIdAndEstablishmentId(accountId, first).orElseThrow().id());
		mvc.perform(get("/api/v1/permission").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isForbidden());
	}

	@Test
	void catalogIsCompleteEvenForAReaderWithOnlyOnePermission() throws Exception {
		String email = member(school(), Set.of("can_permission_read"));
		String response = mvc.perform(get("/api/v1/permission").header(HttpHeaders.AUTHORIZATION, bearer(email)))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		assertTrue(response.contains("can_employee_update"));
		assertTrue(response.contains("can_platform_role_update"));
		assertTrue(response.contains("can_license_suspend"));
	}

	@Test
	void platformScopeAloneDoesNotGrantBusinessPermissionsOrLicenseExemption() throws Exception {
		UUID school = schools.save("NO-" + UUID.randomUUID().toString().substring(0, 8), "Unlicensed").id();
		assertFalse(licenses.canAccessFeature(school, LicenseFeature.SCHOOL_MANAGEMENT, java.time.Instant.now()));
		mvc.perform(get("/api/v1/establishments/" + school + "/subjects").header(HttpHeaders.AUTHORIZATION, bearer("admin-tech")))
			.andExpect(status().isForbidden());
	}

	@Test
	void annualRoutesRequireExplicitContextAndGlobalDashboardRequiresPlatformScope() throws Exception {
		UUID school = school();
		String email = member(school, Set.of("can_dashboard_read", "can_tuition_read"));
		mvc.perform(get("/api/v1/dashboard").header(HttpHeaders.AUTHORIZATION, bearer(email)))
			.andExpect(status().isForbidden());
		mvc.perform(get("/api/v1/establishments/" + school + "/students/" + UUID.randomUUID() + "/tuition-fees")
			.header(HttpHeaders.AUTHORIZATION, bearer(email))).andExpect(status().isNotFound());
	}

	@Autowired StudentRepository students;
	@Autowired TuitionRepository tuition;

	@Test
	void paymentsAndReceiptsRejectAnotherYearWithoutChangingTheLedger() throws Exception {
		UUID school = school();
		UUID firstYear = years.save(school, "First", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)).id();
		UUID secondYear = years.save(school, "Second", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31)).id();
		String email = member(school, Set.of("can_tuition_read", "can_payment_record", "can_payment_receipt_read"));
		var student = students.save(school, "Annual", "Student");
		var type = tuition.saveFeeType(new TuitionFeeTypeDraft(school, "TUITION", "Tuition"));
		var fee = tuition.saveFee(new TuitionFeeDraft(school, firstYear, type.id(), type.code(), type.name(),
			"Annual fee", new java.math.BigDecimal("100.00"), "USD", LocalDate.of(2026, 9, 1), List.of(), null));
		var charge = tuition.saveCharge(fee, student);
		var payment = tuition.savePayment(charge, "REC-AUTH-" + tuition.nextReceiptNumber(), new java.math.BigDecimal("10.00"),
			"CASH", "Cash", null, email);
		String first = "/api/v1/establishments/" + school + "/school-years/" + firstYear;
		String second = "/api/v1/establishments/" + school + "/school-years/" + secondYear;
		String token = bearer(email);
		mvc.perform(get(first + "/students/" + student.id() + "/tuition-fees").header(HttpHeaders.AUTHORIZATION, token))
			.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
		mvc.perform(get(second + "/students/" + student.id() + "/tuition-fees").header(HttpHeaders.AUTHORIZATION, token))
			.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
		mvc.perform(post(second + "/students/" + student.id() + "/tuition-fees/" + charge.id() + "/payments")
			.header(HttpHeaders.AUTHORIZATION, token).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
			.content("{\"amount\":5,\"paymentMethodCode\":\"CASH\"}"))
			.andExpect(status().isNotFound());
		mvc.perform(get(second + "/payments/" + payment.id() + "/receipt").header(HttpHeaders.AUTHORIZATION, token))
			.andExpect(status().isNotFound());
		mvc.perform(get(first + "/payments/" + payment.id() + "/receipt").header(HttpHeaders.AUTHORIZATION, token))
			.andExpect(status().isOk());
		assertEquals(1, tuition.findPayments(charge.id()).size());
	}

	@Test
	void superAdministratorNeedsBothAnExplicitPermissionAndAnExplicitPlatformScope() throws Exception {
		directory.createRole(new Role("SUPER_ADMIN", "Super administrator", null, true, Set.of()));
		String email = "super-" + UUID.randomUUID() + "@example.org";
		UUID accountId = accounts.createEmployeeAccount(email, "super-admin-password-21", Set.of("SUPER_ADMIN"));
		String token = bearer(email);
		mvc.perform(get("/api/v1/admin/license-offers").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isForbidden());
		jdbc.update("INSERT INTO security_role_permissions(role_id,permission_id) SELECT r.id,p.id FROM security_roles r CROSS JOIN security_permissions p WHERE r.code='SUPER_ADMIN' AND p.code='can_license_offer_read'");
		entityManager.clear();
		mvc.perform(get("/api/v1/admin/license-offers").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isForbidden());
		jdbc.update("INSERT INTO security_role_scopes(role_id,scope_code) SELECT id,'PLATFORM' FROM security_roles WHERE code='SUPER_ADMIN'");
		mvc.perform(get("/api/v1/admin/license-offers").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isOk());
		assertNotNull(accountId);
	}

	@Autowired com.galactique.gestionecole.guardians.domain.GuardianRepository guardians;

	@Test
	void staffWhoIsAlsoAGuardianCanUseTheParentPortalEntitlement() throws Exception {
		UUID school = school();
		String email = member(school, Set.of("can_academic_year_read"));
		UUID accountId = jdbc.queryForObject("SELECT id FROM security_user_accounts WHERE username=?", UUID.class, email);
		accounts.addGuardianRole(accountId);
		var guardian = guardians.save("Staff", "Guardian", email);
		guardians.attachUserAccount(guardian.id(), accountId);
		guardians.link(guardian.id(), students.save(school, "Linked", "Child").id());
		years.save(school, "Selected", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
		var offer = licenses.createOffer("PARENT-" + UUID.randomUUID().toString().substring(0, 8), "Parent portal only",
			Set.of(com.galactique.gestionecole.licenses.domain.LicenseFeature.PARENT_PORTAL));
		jdbc.update("UPDATE platform_licenses SET offer_id=? WHERE establishment_id=?", offer.id(), school);
		mvc.perform(get("/api/v1/establishments/" + school + "/school-years").header(HttpHeaders.AUTHORIZATION, bearer(email)))
			.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void platformScopedRoleCannotBeDelegatedThroughAnEstablishmentPosition() throws Exception {
		UUID school = school();
		String email = member(school, Set.of("can_position_create"));
		directory.createRole(new Role("PLATFORM_READER", "Platform reader", null, true,
			Set.of(new Permission("can_student_read", "Read student records"))));
		jdbc.update("INSERT INTO security_role_scopes(role_id,scope_code) SELECT id,'PLATFORM' FROM security_roles WHERE code='PLATFORM_READER'");
		mvc.perform(post("/api/v1/establishments/" + school + "/positions")
			.header(HttpHeaders.AUTHORIZATION, bearer(email)).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
			.content("{\"name\":\"Delegated position\",\"roleCodes\":[\"PLATFORM_READER\"]}"))
			.andExpect(status().isBadRequest());
	}

	@Autowired com.galactique.gestionecole.security.domain.UserAccountRepository userAccounts;

	@Test
	void linkingAnExistingAccountPreservesItsPasswordAndIndependentlyAssignedRoles() throws Exception {
		UUID school = school();
		directory.createRole(new Role("PRESERVED_GLOBAL", "Independent global role", null, true,
			Set.of(new Permission("can_user_read", "Read users"))));
		String email = "preserved-" + UUID.randomUUID() + "@example.org";
		accounts.createEmployeeAccount(email, "preserved-password-21", Set.of("PRESERVED_GLOBAL"));
		String passwordHash = userAccounts.findByUsername(email).orElseThrow().passwordHash();
		var position = positions.save(school, "New membership", Set.of("STAFF"));
		mvc.perform(post("/api/v1/establishments/" + school + "/employees")
			.with(user("admin-tech").authorities(new SimpleGrantedAuthority("can_employee_create")))
			.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
			.content("{\"employeeNumber\":\"NEW-MEMBER\",\"firstName\":\"Existing\",\"lastName\":\"Account\",\"email\":\""
				+ email + "\",\"positionId\":\"" + position.id() + "\",\"directRoleCodes\":[]}"))
			.andExpect(status().isCreated());
		var account = userAccounts.findByUsername(email).orElseThrow();
		assertEquals(Set.of("PRESERVED_GLOBAL", "STAFF"), account.roles().stream().map(Role::code)
			.collect(java.util.stream.Collectors.toSet()));
		assertEquals(passwordHash, account.passwordHash());
	}

	private UUID school() {
		return LicensedEstablishmentFixture.create(schools, licenses,
			"AUTH-" + UUID.randomUUID().toString().substring(0, 8), "Authorization school").id();
	}

	private String member(UUID school, Set<String> permissions) {
		String code = "AUTH_" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
		Set<Permission> selected = directory.findAllPermissions().stream().filter(p -> permissions.contains(p.code()))
			.collect(java.util.stream.Collectors.toSet());
		directory.createRole(new Role(code, "Delegated access", null, true, selected, school));
		var position = positions.save(school, code, Set.of(code));
		String email = code.toLowerCase() + "@example.org";
		var employee = employees.save("E-" + UUID.randomUUID(), "Scoped", "User", email, position.id(), Set.of());
		employees.attachUserAccount(employee.id(), accounts.createEmployeeAccount(email, "authorization-password-21", Set.of(code)));
		return email;
	}

	private String bearer(String username) {
		return "Bearer " + tokens.issue(UsernamePasswordAuthenticationToken.authenticated(username, null,
			List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("can_subject_read")))).value();
	}
}
