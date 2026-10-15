package com.galactique.gestionecole.security.infrastructure.api.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.galactique.gestionecole.security.domain.SecurityAccountProvisioner;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class PasswordControllerTests {
	private final MockMvc mockMvc;
	private final SecurityAccountProvisioner securityAccountProvisioner;

	@Autowired
	PasswordControllerTests(MockMvc mockMvc, SecurityAccountProvisioner securityAccountProvisioner) {
		this.mockMvc = mockMvc;
		this.securityAccountProvisioner = securityAccountProvisioner;
	}

	@Test
	void authenticatedUserCanChangePasswordAndOldRefreshTokensAreRevoked() throws Exception {
		String username = "password-" + UUID.randomUUID() + "@example.org";
		String currentPassword = "old-password-123";
		String newPassword = "new-password-456";
		securityAccountProvisioner.createEmployeeAccount(username, currentPassword, Set.of("STAFF"));

		MvcResult initialLogin = login(username, currentPassword).andExpect(status().isOk()).andReturn();
		String oldRefreshToken = JsonPath.read(initialLogin.getResponse().getContentAsString(), "$.refreshToken");

		mockMvc.perform(put("/api/v1/user/me/password")
			.with(authenticatedUser(username))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"currentPassword\":\"" + currentPassword
				+ "\",\"newPassword\":\"" + newPassword + "\"}"))
			.andExpect(status().isNoContent());

		login(username, currentPassword).andExpect(status().isUnauthorized());
		login(username, newPassword).andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/refresh")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"refreshToken\":\"" + oldRefreshToken + "\"}"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void rejectsIncorrectCurrentPasswordAndWeakOrReusedNewPassword() throws Exception {
		String username = "password-check-" + UUID.randomUUID() + "@example.org";
		String currentPassword = "current-password-789";
		securityAccountProvisioner.createEmployeeAccount(username, currentPassword, Set.of("STAFF"));

		mockMvc.perform(put("/api/v1/user/me/password")
			.with(authenticatedUser(username))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"currentPassword\":\"wrong-password-123\","
				+ "\"newPassword\":\"different-password-456\"}"))
			.andExpect(status().isForbidden());

		mockMvc.perform(put("/api/v1/user/me/password")
			.with(authenticatedUser(username))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"currentPassword\":\"" + currentPassword + "\",\"newPassword\":\"short\"}"))
			.andExpect(status().isBadRequest());

		mockMvc.perform(put("/api/v1/user/me/password")
			.with(authenticatedUser(username))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"currentPassword\":\"" + currentPassword
				+ "\",\"newPassword\":\"" + currentPassword + "\"}"))
			.andExpect(status().isBadRequest());

		login(username, currentPassword).andExpect(status().isOk());
	}

	private org.springframework.test.web.servlet.ResultActions login(String username, String password)
		throws Exception {
		return mockMvc.perform(post("/api/v1/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"login\":\"" + username + "\",\"password\":\"" + password + "\"}"));
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor authenticatedUser(String username) {
		return user(username).authorities(new SimpleGrantedAuthority("ROLE_STAFF"),
			new SimpleGrantedAuthority("STAFF_ACCESS"));
	}
}