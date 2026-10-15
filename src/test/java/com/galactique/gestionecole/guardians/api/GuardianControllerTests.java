package com.galactique.gestionecole.guardians.api;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
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
import com.galactique.gestionecole.security.domain.UserAccountRepository;
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

	@MockitoBean
	private JavaMailSender mailSender;

	@Autowired
	GuardianControllerTests(MockMvc mockMvc, EstablishmentRepository establishmentRepository,
		UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
		this.mockMvc = mockMvc;
		this.establishmentRepository = establishmentRepository;
		this.userAccountRepository = userAccountRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Test
	void invitedGuardianCanOnlySeeLinkedChildrenAcrossTheFamily() throws Exception {
		Establishment establishment = establishmentRepository.save(
			"FAM-" + UUID.randomUUID().toString().substring(0, 8), "Family Test School");
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

	@Test
	void nonParentCannotAccessFamilyPortal() throws Exception {
		mockMvc.perform(get("/api/v1/guardians/me/children")
			.with(user("staff@example.org").authorities(new SimpleGrantedAuthority("STAFF_ACCESS"))))
			.andExpect(status().isForbidden());
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