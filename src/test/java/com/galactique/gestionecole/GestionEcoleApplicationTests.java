package com.galactique.gestionecole;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.galactique.gestionecole.security.domain.UserAccountRepository;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.employees.application.EmployeeInvitationMailer;
import com.galactique.gestionecole.employees.domain.Employee;
import com.galactique.gestionecole.employees.domain.EmployeeRepository;
import com.jayway.jsonpath.JsonPath;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import(GestionEcoleApplicationTests.TestMailConfiguration.class)
class GestionEcoleApplicationTests {
	private static final Map<String, String> invitationTokens = new ConcurrentHashMap<>();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private EstablishmentRepository establishmentRepository;
	
	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void clearInvitationTokens() {
		invitationTokens.clear();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void localVogtFixtureIsSeeded() {
		var establishment = establishmentRepository.findById(
			UUID.fromString("f0320000-0000-4000-8000-000000000001")).orElseThrow();
		assertEquals("VOGT", establishment.code());
		assertEquals("COLLEGE VOGT", establishment.name());
		assertTrue(establishment.enabled());
		var administrator = userAccountRepository.findByUsername("franckyaj@gmail.com").orElseThrow();
		assertTrue(administrator.enabled());
		assertFalse(administrator.locked());
		assertTrue(administrator.passwordHash().startsWith("$2a$10$"));
		assertTrue(administrator.roles().stream().anyMatch(role -> role.code().equals("ESTABLISHMENT_ADMIN")));
		var employee = employeeRepository.findByEmail("franckyaj@gmail.com").orElseThrow();
		assertEquals("FRANCK", employee.firstName());
		assertEquals("YAJE", employee.lastName());
		assertEquals(establishment.id(), employee.establishmentId());
		assertEquals(administrator.id(), employee.userAccountId());
		assertTrue(employee.establishmentAdmin());
		assertEquals(1, jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM enrollments enrollment "
				+ "JOIN students student ON student.id = enrollment.student_id "
				+ "JOIN school_classes school_class ON school_class.id = enrollment.school_class_id "
				+ "JOIN academic_years academic_year ON academic_year.id = enrollment.academic_year_id "
				+ "JOIN school_class_subjects assignment ON assignment.school_class_id = school_class.id "
				+ "JOIN subjects subject ON subject.id = assignment.subject_id "
				+ "WHERE enrollment.establishment_id = ? AND student.first_name = 'Miya' "
				+ "AND student.last_name = 'WANDJA' AND school_class.code = 'SIXIEME' "
				+ "AND subject.code = 'MATH' AND academic_year.name = '2026-2027'",
			Integer.class, establishment.id()));
		assertEquals(1, jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM tuition_fee_types WHERE establishment_id = ? AND code = 'SCOLARITE' AND enabled = TRUE",
			Integer.class, establishment.id()));
		jdbcTemplate.query("SELECT created_at, valid_from, valid_until FROM platform_licenses WHERE establishment_id = ?",
			(resultSet, rowNumber) -> {
				var createdAt = resultSet.getObject("created_at", java.time.OffsetDateTime.class).toInstant();
				var validFrom = resultSet.getObject("valid_from", java.time.OffsetDateTime.class).toInstant();
				var validUntil = resultSet.getObject("valid_until", java.time.OffsetDateTime.class).toInstant();
				assertEquals(java.time.Duration.ofMinutes(2), java.time.Duration.between(createdAt, validFrom));
				assertEquals(java.time.Duration.ofHours(3), java.time.Duration.between(validFrom, validUntil));
				return validFrom;
			}, establishment.id());
		assertEquals(1, jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM platform_licenses WHERE establishment_id = ?", Integer.class, establishment.id()));
	}

	@Test
	void defaultAdminAccountIsSeeded() {
		var adminAccount = userAccountRepository.findByUsername("admin-tech").orElseThrow();

		assertTrue(adminAccount.enabled());
		assertTrue(adminAccount.roles().stream().anyMatch(role -> role.code().equals("ADMIN")));
		assertTrue(passwordEncoder.matches("admin", adminAccount.passwordHash()));
	}

	@Test
	void refreshTokenRotatesAndReuseRevokesItsFamily() throws Exception {
		String loginResponse = mockMvc.perform(post("/api/v1/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"login\":\"admin-tech\",\"password\":\"admin\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.refreshToken").isNotEmpty())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String originalRefreshToken = JsonPath.read(loginResponse, "$.refreshToken");

		String refreshResponse = mockMvc.perform(post("/api/v1/refresh")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"refreshToken\":\"" + originalRefreshToken + "\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isNotEmpty())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String rotatedRefreshToken = JsonPath.read(refreshResponse, "$.refreshToken");
		assertNotEquals(originalRefreshToken, rotatedRefreshToken);

		mockMvc.perform(post("/api/v1/refresh")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"refreshToken\":\"" + originalRefreshToken + "\"}"))
			.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/v1/refresh")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"refreshToken\":\"" + rotatedRefreshToken + "\"}"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void securityDirectoryEndpointsRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/user"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(authorities = "STAFF_ACCESS")
	void securityDirectoryEndpointsRequireAdministratorRole() throws Exception {
		mockMvc.perform(get("/api/v1/user"))
			.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(authorities = "ADMIN_ACCESS")
	void administratorCanReadUsersRolesAndPermissionsWithoutPasswordHashes() throws Exception {
		mockMvc.perform(get("/api/v1/user"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].username").value("admin-tech"))
			.andExpect(jsonPath("$[0].passwordHash").doesNotExist());

		mockMvc.perform(get("/api/v1/role"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].code").value("ADMIN"))
			.andExpect(jsonPath("$[0].permissions[0].code").value("can_establishment_onboard"));

		mockMvc.perform(get("/api/v1/permission"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].code").value("ADMIN_ACCESS"));
	}

	@Test
	void invitedEmployeeCanAcceptInvitationLoginAndOnlySeeTheirEstablishment() throws Exception {
		var firstEstablishment = establishmentRepository.save("SCHOOL-A", "School A");
		var secondEstablishment = establishmentRepository.save("SCHOOL-B", "School B");

		String administratorEmail = "principal@school-a.example";
		String invitationToken = inviteEmployee(
			"STAFF-001", "Alex", "Principal", administratorEmail, "Principal", firstEstablishment.id(), true);
		inviteEmployee("STAFF-002", "Sam", "Teacher", "teacher@school-a.example", "Teacher", firstEstablishment.id(), false);
		inviteEmployee("STAFF-003", "Pat", "Teacher", "teacher@school-b.example", "Teacher", secondEstablishment.id(), false);

		String password = "school-employee-secure-password";
		mockMvc.perform(post("/api/v1/employees/invitations/accept")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"token\":\"" + invitationToken + "\",\"password\":\"" + password + "\"}"))
			.andExpect(status().isNoContent());

		String loginResponse = mockMvc.perform(post("/api/v1/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"login\":\"" + administratorEmail + "\",\"password\":\"" + password + "\"}"))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String accessToken = JsonPath.read(loginResponse, "$.accessToken");

		mockMvc.perform(get("/api/v1/employees/me").header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value(administratorEmail));

		mockMvc.perform(get("/api/v1/employees").header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)));

		mockMvc.perform(get("/api/v1/establishments").header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].id").value(firstEstablishment.id().toString()));

		mockMvc.perform(post("/api/v1/employees/invitations/accept")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"token\":\"" + invitationToken + "\",\"password\":\"" + password + "\"}"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(authorities = "STAFF_ACCESS")
	void regularEmployeeCannotBrowseEmployeeOrEstablishments() throws Exception {
		mockMvc.perform(get("/api/v1/employees"))
			.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/v1/establishments"))
			.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/v1/establishments/" + UUID.randomUUID() + "/positions"))
			.andExpect(status().isForbidden());
	}

	@Test
	void employeeCanBeUpdatedAndSoftDeletedWithinEstablishmentScope() throws Exception {
		var firstEstablishment = establishmentRepository.save("SCHOOL-C", "School C");
		var secondEstablishment = establishmentRepository.save("SCHOOL-D", "School D");
		String principalEmail = "principal-c@example.org";
		String teacherEmail = "teacher-c@example.org";
		String otherTeacherEmail = "teacher-d@example.org";

		String principalInvitation = inviteEmployee("STAFF-C-001", "Casey", "Principal", principalEmail,
			"Principal", firstEstablishment.id(), true);
		String teacherInvitation = inviteEmployee("STAFF-C-002", "Taylor", "Teacher", teacherEmail,
			"Teacher", firstEstablishment.id(), false);
		String otherInvitation = inviteEmployee("STAFF-D-001", "Jamie", "Teacher", otherTeacherEmail,
			"Teacher", secondEstablishment.id(), false);

		acceptEmployeeInvitation(principalInvitation, "principal-c-secure-password");
		acceptEmployeeInvitation(teacherInvitation, "teacher-c-secure-password");
		acceptEmployeeInvitation(otherInvitation, "teacher-d-secure-password");

		String principalToken = login(principalEmail, "principal-c-secure-password");
		Employee teacher = employeeRepository.findByEmail(teacherEmail).orElseThrow();
		Employee otherTeacher = employeeRepository.findByEmail(otherTeacherEmail).orElseThrow();
		UUID seniorPositionId = createPosition(firstEstablishment.id(), "Senior Teacher", "[]");

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
			.patch("/api/v1/employees/" + teacher.id())
			.header("Authorization", "Bearer " + principalToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"positionId\":\"" + seniorPositionId + "\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.positionName").value("Senior Teacher"));

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
			.put("/api/v1/employees/" + teacher.id() + "/roles")
			.header("Authorization", "Bearer " + principalToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"roleCodes\":[\"STAFF\",\"ESTABLISHMENT_ADMIN\"]}"))
			.andExpect(status().isBadRequest());

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
			.put("/api/v1/employees/" + teacher.id() + "/roles")
			.header("Authorization", "Bearer " + principalToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"roleCodes\":[\"STAFF\"]}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.roles[0]").value("STAFF"));

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
			.put("/api/v1/establishments/" + firstEstablishment.id() + "/positions/"
				+ seniorPositionId + "/roles")
			.header("Authorization", "Bearer " + principalToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"roleCodes\":[\"STAFF\",\"ESTABLISHMENT_ADMIN\"]}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.roleCodes", hasSize(2)));
		mockMvc.perform(get("/api/v1/establishments/" + firstEstablishment.id() + "/positions")
			.header("Authorization", "Bearer " + principalToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[1].roleCodes", hasSize(2)));
		Employee updatedTeacher = employeeRepository.findByEmail(teacherEmail).orElseThrow();
		assertEquals(Set.of("STAFF"), updatedTeacher.directRoleCodes());
		assertEquals(Set.of("STAFF", "ESTABLISHMENT_ADMIN"), updatedTeacher.roleCodes());
		String promotedTeacherToken = login(teacherEmail, "teacher-c-secure-password");
		mockMvc.perform(get("/api/v1/establishments").header("Authorization", "Bearer " + promotedTeacherToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)));

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
			.patch("/api/v1/employees/" + otherTeacher.id())
			.header("Authorization", "Bearer " + principalToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"positionId\":\"" + teacher.positionId() + "\"}"))
			.andExpect(status().isNotFound());

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
			.delete("/api/v1/employees/" + teacher.id())
			.header("Authorization", "Bearer " + principalToken))
			.andExpect(status().isNoContent());
		assertFalse(userAccountRepository.findByUsername(teacherEmail).orElseThrow().enabled());

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
			.delete("/api/v1/establishments/" + firstEstablishment.id())
			.with(user("admin-tech").authorities(new SimpleGrantedAuthority("ADMIN_ACCESS"))))
			.andExpect(status().isNoContent());
		assertFalse(userAccountRepository.findByUsername(principalEmail).orElseThrow().enabled());
		assertFalse(employeeRepository.findByEmail(principalEmail).orElseThrow().enabled());
		assertTrue(userAccountRepository.findByUsername(otherTeacherEmail).orElseThrow().enabled());
	}

	private void acceptEmployeeInvitation(String token, String password) throws Exception {
		mockMvc.perform(post("/api/v1/employees/invitations/accept")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"token\":\"" + token + "\",\"password\":\"" + password + "\"}"))
			.andExpect(status().isNoContent());
	}

	private String login(String username, String password) throws Exception {
		String response = mockMvc.perform(post("/api/v1/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"login\":\"" + username + "\",\"password\":\"" + password + "\"}"))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(response, "$.accessToken");
	}

	private String inviteEmployee(String employeeNumber, String firstName, String lastName, String email,
		String position, UUID establishmentId, boolean establishmentAdmin) throws Exception {
		String inheritedRoles = establishmentAdmin ? "[\"ESTABLISHMENT_ADMIN\"]" : "[]";
		UUID positionId = createPosition(establishmentId, position, inheritedRoles);
		String requestBody = """
			{"employeeNumber":"%s","firstName":"%s","lastName":"%s","email":"%s","positionId":"%s","directRoleCodes":["STAFF"]}
			""".formatted(employeeNumber, firstName, lastName, email, positionId);

		mockMvc.perform(post("/api/v1/employees")
			.with(user("admin-tech").authorities(new SimpleGrantedAuthority("ADMIN_ACCESS")))
			.contentType(MediaType.APPLICATION_JSON)
			.content(requestBody))
			.andExpect(status().isCreated());

		return invitationTokens.get(email);
	}

	private UUID createPosition(UUID establishmentId, String name, String rolesJson) throws Exception {
		String response = mockMvc.perform(post("/api/v1/establishments/" + establishmentId + "/positions")
			.with(user("admin-tech").authorities(
				new SimpleGrantedAuthority("ADMIN_ACCESS"), new SimpleGrantedAuthority("can_position_create")))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"" + name + "\",\"roleCodes\":" + rolesJson + "}"))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return UUID.fromString(JsonPath.read(response, "$.id"));
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestMailConfiguration {
		@Bean
		@Primary
		EmployeeInvitationMailer employeeInvitationMailer() {
			return (employee, invitationToken) -> invitationTokens.put(employee.email(), invitationToken);
		}
	}

}
