package com.galactique.gestionecole.establishments.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.galactique.gestionecole.employees.domain.EmployeeRepository;
import com.galactique.gestionecole.security.domain.UserAccountRepository;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = {
	"management.health.mail.enabled=false",
	"spring.datasource.url=jdbc:h2:mem:tabi_onboarding_integration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TabiEstablishmentsIntegrationTests {
	private static final String EMAIL = "franckyaj@gmail.com";
	private static final String FIRST_ESTABLISHMENT = """
		{"code":"TABI","name":"COLLEGE JEAN TABI","administrator":{"employeeNumber":"2222","firstName":"Franck Arnold","lastName":"YAJE WANDJA","email":"franckyaj@gmail.com"}}
		""";
	private static final String SECOND_ESTABLISHMENT = """
		{"code":"TABI2","name":"COLLEGE JEAN2 TABI","administrator":{"employeeNumber":"2222","firstName":"Franck Arnold","lastName":"YAJE WANDJA","email":"franckyaj@gmail.com"}}
		""";

	@Autowired private MockMvc mockMvc;
	@Autowired private JdbcTemplate jdbc;
	@Autowired private EmployeeRepository employees;
	@Autowired private UserAccountRepository accounts;
	@MockitoBean private JavaMailSender mailSender;

	@Test
	void createsBothEstablishmentsWithTheSameAdministratorOnceTheFirstInvitationIsAccepted() throws Exception {
		String platformToken = login("admin-tech", "admin");

		String firstResponse = mockMvc.perform(post("/api/v1/establishments")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + platformToken)
			.contentType(MediaType.APPLICATION_JSON).content(FIRST_ESTABLISHMENT))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.establishment.code").value("TABI"))
			.andExpect(jsonPath("$.establishment.name").value("COLLEGE JEAN TABI"))
			.andExpect(jsonPath("$.administratorInvitation.status").value("INVITATION_SENT"))
			.andReturn().getResponse().getContentAsString();
		UUID firstSchoolId = UUID.fromString(JsonPath.read(firstResponse, "$.establishment.id"));
		UUID firstEmployeeId = UUID.fromString(JsonPath.read(firstResponse, "$.administratorInvitation.employeeId"));
		assertNull(employees.findById(firstEmployeeId).orElseThrow().userAccountId());
		assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM security_user_accounts WHERE username = ?", Integer.class, EMAIL));

		// The second JSON is submitted immediately, without accepting the first invitation.
		mockMvc.perform(post("/api/v1/establishments")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + platformToken)
			.contentType(MediaType.APPLICATION_JSON).content(SECOND_ESTABLISHMENT))
			.andExpect(status().isConflict());
		assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM establishments WHERE code = 'TABI2'", Integer.class));
		assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM establishments", Integer.class));
		assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM positions", Integer.class));
		assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM employees WHERE email = ?", Integer.class, EMAIL));

		ArgumentCaptor<SimpleMailMessage> invitation = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender).send(invitation.capture());
		assertEquals(EMAIL, invitation.getValue().getTo()[0]);
		String invitationText = invitation.getValue().getText();
		assertNotNull(invitationText);
		String invitationToken = invitationText.substring(invitationText.indexOf("#token=") + "#token=".length()).split("\\s")[0];
		mockMvc.perform(post("/api/v1/employees/invitations/accept")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"token\":\"" + invitationToken + "\",\"password\":\"TabiIntegrationPassword123!\"}"))
			.andExpect(status().isNoContent());
		var administratorAccount = accounts.findByUsername(EMAIL).orElseThrow();

		// Retry the exact same second JSON after the administrator account has been activated.
		String secondResponse = mockMvc.perform(post("/api/v1/establishments")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + platformToken)
			.contentType(MediaType.APPLICATION_JSON).content(SECOND_ESTABLISHMENT))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.establishment.code").value("TABI2"))
			.andExpect(jsonPath("$.establishment.name").value("COLLEGE JEAN2 TABI"))
			.andExpect(jsonPath("$.administratorInvitation.status").value("ACCOUNT_LINKED"))
			.andReturn().getResponse().getContentAsString();
		UUID secondSchoolId = UUID.fromString(JsonPath.read(secondResponse, "$.establishment.id"));
		UUID secondEmployeeId = UUID.fromString(JsonPath.read(secondResponse, "$.administratorInvitation.employeeId"));
		assertNotEquals(firstSchoolId, secondSchoolId);
		assertNotEquals(firstEmployeeId, secondEmployeeId);
		for (UUID schoolId : java.util.List.of(firstSchoolId, secondSchoolId)) {
			var membership = employees.findByEmailAndEstablishmentId(EMAIL, schoolId).orElseThrow();
			assertEquals("2222", membership.employeeNumber());
			assertEquals("Franck Arnold", membership.firstName());
			assertEquals("YAJE WANDJA", membership.lastName());
			assertEquals(administratorAccount.id(), membership.userAccountId());
		}
		assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM security_user_accounts WHERE username = ?", Integer.class, EMAIL));
		assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM employees WHERE email = ?", Integer.class, EMAIL));
		assertEquals(administratorAccount.passwordHash(), accounts.findByUsername(EMAIL).orElseThrow().passwordHash());

		String administratorToken = login(EMAIL, "TabiIntegrationPassword123!");
		mockMvc.perform(get("/api/v1/establishments")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + administratorToken))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[*].code", hasItem("TABI")))
			.andExpect(jsonPath("$[*].code", hasItem("TABI2")));
		verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
	}

	private String login(String login, String password) throws Exception {
		String response = mockMvc.perform(post("/api/v1/login").contentType(MediaType.APPLICATION_JSON)
			.content("{\"login\":\"" + login + "\",\"password\":\"" + password + "\"}"))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.accessToken");
	}
}
