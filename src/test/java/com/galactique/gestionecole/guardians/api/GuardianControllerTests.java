package com.galactique.gestionecole.guardians.api;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;
import com.galactique.gestionecole.support.LicensedEstablishmentFixture;
import com.galactique.gestionecole.security.domain.UserAccountRepository;
import com.galactique.gestionecole.security.domain.SecurityAccountProvisioner;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = "management.health.mail.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class GuardianControllerTests {
	private static final Pattern INVITATION_TOKEN = Pattern.compile("#token=([A-Za-z0-9_-]+)");

	private final MockMvc mockMvc;
	private final EstablishmentRepository establishmentRepository;
	private final UserAccountRepository userAccountRepository;
	private final PasswordEncoder passwordEncoder;
	private final LicenseApplicationService licenseApplicationService;
	private final SecurityAccountProvisioner securityAccountProvisioner;

	@MockitoBean
	private JavaMailSender mailSender;

	@Autowired
	GuardianControllerTests(MockMvc mockMvc, EstablishmentRepository establishmentRepository,
		UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder,
		LicenseApplicationService licenseApplicationService, SecurityAccountProvisioner securityAccountProvisioner) {
		this.mockMvc = mockMvc;
		this.establishmentRepository = establishmentRepository;
		this.userAccountRepository = userAccountRepository;
		this.passwordEncoder = passwordEncoder;
		this.licenseApplicationService = licenseApplicationService;
		this.securityAccountProvisioner = securityAccountProvisioner;
	}

	@Test
	void invitedGuardianCanOnlySeeLinkedChildrenAcrossTheFamily() throws Exception {
		Establishment establishment = LicensedEstablishmentFixture.create(establishmentRepository,
			licenseApplicationService, "FAM-" + UUID.randomUUID().toString().substring(0, 8), "Family Test School");
		String firstStudentId = createStudent(establishment, "Family A");
		String secondStudentId = createStudent(establishment, "Family B");
		createStudent(establishment, "Family C");
		String email = "guardian-" + UUID.randomUUID() + "@example.org";

		MvcResult linkResult = mockMvc.perform(post(studentGuardiansUrl(establishment.id(), firstStudentId))
			.with(admin("can_guardian_link"))
			.contentType(MediaType.APPLICATION_JSON)
			.content(guardianRequest(email)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.accessStatus").value("INVITATION_SENT"))
			.andReturn();
		String guardianId = JsonPath.read(linkResult.getResponse().getContentAsString(), "$.guardian.id");
		String invitationToken = extractInvitationToken(email);

		mockMvc.perform(post("/api/v1/guardians/invitations/accept")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"token\":\"" + invitationToken + "\",\"password\":\"parent-password-21\"}"))
			.andExpect(status().isNoContent());
		var parentAccount = userAccountRepository.findByUsername(email).orElseThrow();
		assertTrue(passwordEncoder.matches("parent-password-21", parentAccount.passwordHash()));
		assertEquals("PARENT", parentAccount.roles().iterator().next().code());

		mockMvc.perform(post(studentGuardiansUrl(establishment.id(), secondStudentId))
			.with(admin("can_guardian_link"))
			.contentType(MediaType.APPLICATION_JSON)
			.content(guardianRequest(email)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.guardian.id").value(guardianId))
			.andExpect(jsonPath("$.accessStatus").value("ACCESS_ALREADY_ACTIVE"));

		MvcResult login = mockMvc.perform(post("/api/v1/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"login\":\"" + email + "\",\"password\":\"parent-password-21\"}"))
			.andExpect(status().isOk())
			.andReturn();
		String accessToken = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");

		mockMvc.perform(get("/api/v1/guardians/me/children")
			.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[*].firstName", containsInAnyOrder("Family A", "Family B")));
	}

	@ParameterizedTest
	@ValueSource(strings = {"ESTABLISHMENT_ADMIN", "STAFF"})
	void existingAccountCanBeGuardianAcrossSchoolsWithoutLosingItsRolesOrPassword(String roleCode) throws Exception {
		Establishment firstSchool = createEstablishment();
		Establishment secondSchool = createEstablishment();
		String firstStudentId = createStudent(firstSchool, "First child");
		String secondStudentId = createStudent(secondSchool, "Second child");
		createStudent(firstSchool, "Unlinked child");
		String email = "existing-" + UUID.randomUUID() + "@example.org";
		UUID accountId = securityAccountProvisioner.createEmployeeAccount(email, "existing-password-21", Set.of(roleCode));
		String passwordHash = userAccountRepository.findByUsername(email).orElseThrow().passwordHash();

		MvcResult firstLink = mockMvc.perform(post(studentGuardiansUrl(firstSchool.id(), firstStudentId))
			.with(admin("can_guardian_link"))
			.contentType(MediaType.APPLICATION_JSON)
			.content(guardianRequest(email.toUpperCase(java.util.Locale.ROOT))))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.accessStatus").value("ACCESS_ALREADY_ACTIVE"))
			.andReturn();
		String guardianId = JsonPath.read(firstLink.getResponse().getContentAsString(), "$.guardian.id");
		mockMvc.perform(post(studentGuardiansUrl(secondSchool.id(), secondStudentId))
			.with(admin("can_guardian_link"))
			.contentType(MediaType.APPLICATION_JSON)
			.content(guardianRequest(email)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.guardian.id").value(guardianId))
			.andExpect(jsonPath("$.accessStatus").value("ACCESS_ALREADY_ACTIVE"));
		mockMvc.perform(post(studentGuardiansUrl(firstSchool.id(), firstStudentId))
			.with(admin("can_guardian_link"))
			.contentType(MediaType.APPLICATION_JSON)
			.content(guardianRequest(email)))
			.andExpect(status().isConflict());

		var account = userAccountRepository.findByUsername(email).orElseThrow();
		assertEquals(accountId, account.id());
		assertEquals(passwordHash, account.passwordHash());
		assertEquals(Set.of(roleCode, "PARENT"), account.roles().stream()
			.map(role -> role.code()).collect(java.util.stream.Collectors.toSet()));
		verifyNoInteractions(mailSender);
		String accessToken = login(email, "existing-password-21");
		mockMvc.perform(get("/api/v1/guardians/me/children")
			.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].id", containsInAnyOrder(firstStudentId, secondStudentId)));

		mockMvc.perform(delete(studentGuardiansUrl(firstSchool.id(), firstStudentId) + "/" + guardianId)
			.with(admin("can_guardian_unlink")))
			.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/guardians/me/children")
			.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].id").value(secondStudentId));
		mockMvc.perform(get("/api/v1/guardians/me/children/" + firstStudentId
			+ "/summary?academicYearId=" + UUID.randomUUID())
			.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isNotFound());
	}

	@Test
	void employeeRoleAndEmailUpdatesPreserveGuardianAccessAndIdentity() throws Exception {
		Establishment school = createEstablishment();
		String firstStudentId = createStudent(school, "First child");
		String secondStudentId = createStudent(school, "Second child");
		String email = "employee-parent-" + UUID.randomUUID() + "@example.org";
		UUID accountId = securityAccountProvisioner.createEmployeeAccount(email, "existing-password-21",
			Set.of("ESTABLISHMENT_ADMIN"));
		MvcResult link = mockMvc.perform(post(studentGuardiansUrl(school.id(), firstStudentId))
			.with(admin("can_guardian_link"))
			.contentType(MediaType.APPLICATION_JSON).content(guardianRequest(email)))
			.andExpect(status().isCreated()).andReturn();
		String guardianId = JsonPath.read(link.getResponse().getContentAsString(), "$.guardian.id");

		securityAccountProvisioner.updateEmployeeRoles(accountId, Set.of("STAFF"));
		assertEquals(Set.of("STAFF", "PARENT"), userAccountRepository.findByUsername(email).orElseThrow()
			.roles().stream().map(role -> role.code()).collect(java.util.stream.Collectors.toSet()));
		String updatedEmail = "updated-" + UUID.randomUUID() + "@example.org";
		securityAccountProvisioner.updateEmployeeAccount(accountId, updatedEmail, Set.of("ESTABLISHMENT_ADMIN"));
		String accessToken = login(updatedEmail, "existing-password-21");
		mockMvc.perform(get("/api/v1/guardians/me/children")
			.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].id").value(firstStudentId));
		mockMvc.perform(post(studentGuardiansUrl(school.id(), secondStudentId))
			.with(admin("can_guardian_link"))
			.contentType(MediaType.APPLICATION_JSON).content(guardianRequest(updatedEmail)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.guardian.id").value(guardianId));
		verifyNoInteractions(mailSender);
	}

	@Test
	void reusedEmployeeEmailCannotRedirectAGuardianLinkToAnotherAccount() throws Exception {
		Establishment school = createEstablishment();
		String firstStudentId = createStudent(school, "First child");
		String secondStudentId = createStudent(school, "Second child");
		String email = "reused-" + UUID.randomUUID() + "@example.org";
		UUID originalAccountId = securityAccountProvisioner.createEmployeeAccount(email, "original-password-21",
			Set.of("STAFF"));
		mockMvc.perform(post(studentGuardiansUrl(school.id(), firstStudentId))
			.with(admin("can_guardian_link"))
			.contentType(MediaType.APPLICATION_JSON).content(guardianRequest(email)))
			.andExpect(status().isCreated());
		String updatedEmail = "updated-" + UUID.randomUUID() + "@example.org";
		securityAccountProvisioner.updateEmployeeAccount(originalAccountId, updatedEmail, Set.of("STAFF"));
		securityAccountProvisioner.createEmployeeAccount(email, "different-password-21", Set.of("STAFF"));

		mockMvc.perform(post(studentGuardiansUrl(school.id(), secondStudentId))
			.with(admin("can_guardian_link"))
			.contentType(MediaType.APPLICATION_JSON).content(guardianRequest(email)))
			.andExpect(status().isConflict());
		String accessToken = login(updatedEmail, "original-password-21");
		mockMvc.perform(get("/api/v1/guardians/me/children")
			.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].id").value(firstStudentId));
		assertEquals(Set.of("STAFF"), userAccountRepository.findByUsername(email).orElseThrow().roles()
			.stream().map(role -> role.code()).collect(java.util.stream.Collectors.toSet()));
		verifyNoInteractions(mailSender);
	}

	@Test
	void pendingGuardianInvitationCannotResetAnExistingAccountsPassword() throws Exception {
		Establishment school = createEstablishment();
		String firstStudentId = createStudent(school, "First child");
		String secondStudentId = createStudent(school, "Second child");
		String email = "pending-" + UUID.randomUUID() + "@example.org";
		mockMvc.perform(post(studentGuardiansUrl(school.id(), firstStudentId))
			.with(admin("can_guardian_link"))
			.contentType(MediaType.APPLICATION_JSON).content(guardianRequest(email)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.accessStatus").value("INVITATION_SENT"));
		String invitationToken = extractInvitationToken(email);
		UUID accountId = securityAccountProvisioner.createEmployeeAccount(email, "existing-password-21", Set.of("STAFF"));
		mockMvc.perform(post(studentGuardiansUrl(school.id(), secondStudentId))
			.with(admin("can_guardian_link"))
			.contentType(MediaType.APPLICATION_JSON).content(guardianRequest(email)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.accessStatus").value("ACCESS_ALREADY_ACTIVE"));
		mockMvc.perform(post("/api/v1/guardians/invitations/accept")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"token\":\"" + invitationToken + "\",\"password\":\"replacement-password-21\"}"))
			.andExpect(status().isUnauthorized());
		var account = userAccountRepository.findByUsername(email).orElseThrow();
		assertEquals(accountId, account.id());
		assertTrue(passwordEncoder.matches("existing-password-21", account.passwordHash()));
	}

	@Test
	void nonParentCannotAccessFamilyPortal() throws Exception {
		mockMvc.perform(get("/api/v1/guardians/me/children")
			.with(user("staff@example.org").authorities(new SimpleGrantedAuthority("STAFF_ACCESS"))))
			.andExpect(status().isForbidden());
	}

	private Establishment createEstablishment() {
		return LicensedEstablishmentFixture.create(establishmentRepository, licenseApplicationService,
			"FAM-" + UUID.randomUUID().toString().substring(0, 8), "Family Test School");
	}

	private String login(String email, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"login\":\"" + email + "\",\"password\":\"" + password + "\"}"))
			.andExpect(status().isOk()).andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
	}

	private String createStudent(Establishment establishment, String firstName) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/establishments/" + establishment.id() + "/students")
			.with(admin("can_student_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"firstName\":\"" + firstName + "\",\"lastName\":\"Student\"}"))
			.andExpect(status().isCreated())
			.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String extractInvitationToken(String email) throws Exception {
		org.mockito.ArgumentCaptor<SimpleMailMessage> captor =
			org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender).send(captor.capture());
		SimpleMailMessage message = captor.getValue();
		if (!email.equals(message.getTo()[0])) {
			throw new AssertionError("Invitation should be addressed to the linked guardian");
		}
		Matcher matcher = INVITATION_TOKEN.matcher(message.getText());
		if (!matcher.find()) {
			throw new AssertionError("Invitation email should contain a token fragment");
		}
		return matcher.group(1);
	}

	private static String guardianRequest(String email) {
		return "{\"firstName\":\"Family\",\"lastName\":\"Guardian\",\"email\":\"" + email + "\"}";
	}

	private static String studentGuardiansUrl(UUID establishmentId, String studentId) {
		return "/api/v1/establishments/" + establishmentId + "/students/" + studentId + "/guardians";
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor admin(String permission) {
		return user("admin-tech").authorities(new SimpleGrantedAuthority("ADMIN_ACCESS"),
			new SimpleGrantedAuthority(permission));
	}
}
