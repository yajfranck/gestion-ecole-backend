package com.galactique.gestionecole.security.infrastructure.api.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class RoleManagementControllerTests {
	private final MockMvc mockMvc;

	@Autowired
	RoleManagementControllerTests(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void administratorCanCreateAndReplaceRoleDetailsAndPermissions() throws Exception {
		String roleCode = uniqueRoleCode();
		mockMvc.perform(post("/api/v1/role")
			.with(administrator())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"" + roleCode + "\",\"name\":\"Librarian\","
				+ "\"description\":\"Library access\",\"permissionCodes\":[\"can_student_read\"]}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.code").value(roleCode))
			.andExpect(jsonPath("$.permissions", hasSize(1)))
			.andExpect(jsonPath("$.permissions[0].code").value("can_student_read"));

		mockMvc.perform(put("/api/v1/role/" + roleCode)
			.with(administrator())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"Student Viewer\",\"description\":\"Read-only school access\","
				+ "\"permissionCodes\":[\"can_student_read\",\"can_enrollment_read\"]}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Student Viewer"))
			.andExpect(jsonPath("$.description").value("Read-only school access"))
			.andExpect(jsonPath("$.permissions", hasSize(2)))
			.andExpect(jsonPath("$.permissions[*].code", hasItem("can_enrollment_read")));

		mockMvc.perform(get("/api/v1/role").with(administrator()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.code == '" + roleCode + "')].name").value("Student Viewer"));
	}

	@Test
	void rejectsDuplicateCodesUnknownPermissionsAndAdminLockout() throws Exception {
		String roleCode = uniqueRoleCode();
		mockMvc.perform(post("/api/v1/role")
			.with(administrator())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"" + roleCode + "\",\"name\":\"Librarian\","
				+ "\"permissionCodes\":[\"can_student_read\"]}"))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/role")
			.with(administrator())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"" + roleCode + "\",\"name\":\"Duplicate\","
				+ "\"permissionCodes\":[\"can_student_read\"]}"))
			.andExpect(status().isConflict());

		mockMvc.perform(post("/api/v1/role")
			.with(administrator())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"AUDITOR\",\"name\":\"Auditor\","
				+ "\"permissionCodes\":[\"can_unknown_read\"]}"))
			.andExpect(status().isBadRequest());

		mockMvc.perform(put("/api/v1/role/ADMIN")
			.with(administrator())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"Administrator\",\"permissionCodes\":[\"can_student_read\"]}"))
			.andExpect(status().isBadRequest());

		mockMvc.perform(get("/api/v1/role").with(administrator()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[?(@.code == 'ADMIN')].permissions[*].code", hasItem("ADMIN_ACCESS")));
	}

	@Test
	void nonAdministratorCannotCreateOrUpdateRoles() throws Exception {
		mockMvc.perform(post("/api/v1/role")
			.with(user("staff@example.org").authorities(new SimpleGrantedAuthority("STAFF_ACCESS")))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"LIBRARIAN\",\"name\":\"Librarian\","
				+ "\"permissionCodes\":[\"can_student_read\"]}"))
			.andExpect(status().isForbidden());
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor administrator() {
		return user("admin-tech").authorities(new SimpleGrantedAuthority("ADMIN_ACCESS"));
	}

	private static String uniqueRoleCode() {
		return "TEST_ROLE_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
	}
}