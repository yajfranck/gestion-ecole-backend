package com.galactique.gestionecole.establishments.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "management.health.mail.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class EstablishmentOnboardingControllerTests {
	private final MockMvc mockMvc;

	@MockitoBean
	private JavaMailSender mailSender;

	@Autowired
	EstablishmentOnboardingControllerTests(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void platformAdminCreatesEstablishmentAndInvitesItsAdministratorInOneRequest() throws Exception {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		mockMvc.perform(post("/api/v1/establishments")
			.with(platformAdmin())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"ONB-" + suffix + "\",\"name\":\"Onboarding School\","
				+ "\"administrator\":{\"employeeNumber\":\"ADMIN-" + suffix
				+ "\",\"firstName\":\"School\",\"lastName\":\"Admin\","
				+ "\"email\":\"school-admin-" + suffix + "@example.org\"}}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.establishment.code").value(("ONB-" + suffix).toUpperCase()))
			.andExpect(jsonPath("$.administratorInvitation.status").value("INVITATION_SENT"));

		mockMvc.perform(get("/api/v1/establishments").with(establishmentReader()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].code", hasItem(("ONB-" + suffix).toUpperCase())));

		verify(mailSender).send(any(SimpleMailMessage.class));
	}

	@Test
	void defaultAdminAccountCanOnboardAnEstablishmentWithItsActualPermissions() throws Exception {
		var login = mockMvc.perform(post("/api/v1/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"login\":\"admin-tech\",\"password\":\"admin\"}"))
			.andExpect(status().isOk()).andReturn();
		String accessToken = com.jayway.jsonpath.JsonPath.read(
			login.getResponse().getContentAsString(), "$.accessToken");
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		mockMvc.perform(post("/api/v1/establishments")
			.header("Authorization", "Bearer " + accessToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"DEF-" + suffix + "\",\"name\":\"Default Admin School\","
				+ "\"administrator\":{\"employeeNumber\":\"DEF-ADM-" + suffix
				+ "\",\"firstName\":\"Default\",\"lastName\":\"Admin\","
				+ "\"email\":\"default-admin-" + suffix + "@example.org\"}}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.establishment.code").value(("DEF-" + suffix).toUpperCase(java.util.Locale.ROOT)));
	}

	@Test
	void nonPlatformAdminCannotOnboardAnEstablishment() throws Exception {
		mockMvc.perform(post("/api/v1/establishments")
			.with(user("staff@example.org").authorities(new SimpleGrantedAuthority("STAFF_ACCESS")))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"DENIED-SCHOOL\",\"name\":\"Denied School\","
				+ "\"administrator\":{\"employeeNumber\":\"DENIED-ADMIN\","
				+ "\"firstName\":\"School\",\"lastName\":\"Admin\",\"email\":\"denied@example.org\"}}"))
			.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/v1/establishments")
			.with(user("staff@example.org").authorities(new SimpleGrantedAuthority("STAFF_ACCESS"))))
			.andExpect(status().isForbidden());
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor platformAdmin() {
		return user("admin-tech").authorities(new SimpleGrantedAuthority("ROLE_ADMIN"),
			new SimpleGrantedAuthority("can_establishment_onboard"));
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor establishmentReader() {
		return user("admin-tech").authorities(new SimpleGrantedAuthority("ROLE_ADMIN"),
			new SimpleGrantedAuthority("can_establishment_read"));
	}
}