package com.galactique.gestionecole.positions.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
import org.springframework.test.web.servlet.MvcResult;

import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class PositionRoleAssignmentTests {
	private final MockMvc mockMvc;
	private final EstablishmentRepository establishmentRepository;

	@Autowired
	PositionRoleAssignmentTests(MockMvc mockMvc, EstablishmentRepository establishmentRepository) {
		this.mockMvc = mockMvc;
		this.establishmentRepository = establishmentRepository;
	}

	@Test
	void positionCanReceiveMultipleCustomRolesAndReplaceThem() throws Exception {
		String customRoleCode = "POSITION_ROLE_" + UUID.randomUUID().toString()
			.replace("-", "").substring(0, 8).toUpperCase();
		mockMvc.perform(post("/api/v1/role")
			.with(administrator("ADMIN_ACCESS"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"" + customRoleCode + "\",\"name\":\"Custom Staff\","
				+ "\"permissionCodes\":[\"can_student_read\"]}"))
			.andExpect(status().isCreated());
		String globalAccessRoleCode = "CUSTOM_ADMIN_" + UUID.randomUUID().toString()
			.replace("-", "").substring(0, 8).toUpperCase();
		mockMvc.perform(post("/api/v1/role")
			.with(administrator("ADMIN_ACCESS"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"" + globalAccessRoleCode + "\",\"name\":\"Custom Global Access\","
				+ "\"permissionCodes\":[\"ADMIN_ACCESS\"]}"))
			.andExpect(status().isCreated());

		Establishment establishment = establishmentRepository.save(
			"POS-" + UUID.randomUUID().toString().substring(0, 8), "Position Role School");
		Establishment otherEstablishment = establishmentRepository.save(
			"POS-" + UUID.randomUUID().toString().substring(0, 8), "Other Position Role School");
		String foreignRoleCode = "SCHOOL_ROLE_" + UUID.randomUUID().toString()
			.replace("-", "").substring(0, 8).toUpperCase();
		mockMvc.perform(post("/api/v1/establishments/" + otherEstablishment.id() + "/roles")
			.with(administrator("ADMIN_ACCESS"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"" + foreignRoleCode + "\",\"name\":\"Other School Role\","
				+ "\"permissionCodes\":[\"can_student_read\"]}"))
			.andExpect(status().isCreated());
		String positionsUrl = "/api/v1/establishments/" + establishment.id() + "/positions";
		MvcResult createdPosition = mockMvc.perform(post(positionsUrl)
			.with(administrator("can_position_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"Teaching Assistant\",\"roleCodes\":[\"STAFF\",\""
				+ customRoleCode + "\"]}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.roleCodes", hasSize(2)))
			.andExpect(jsonPath("$.roleCodes", hasItem(customRoleCode)))
			.andReturn();
		String positionId = JsonPath.read(createdPosition.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(put(positionsUrl + "/" + positionId + "/roles")
			.with(administrator("can_position_assign_roles"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"roleCodes\":[\"ESTABLISHMENT_ADMIN\",\"" + customRoleCode + "\"]}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.roleCodes", hasSize(2)))
			.andExpect(jsonPath("$.roleCodes", hasItem("ESTABLISHMENT_ADMIN")))
			.andExpect(jsonPath("$.roleCodes", hasItem(customRoleCode)));

		mockMvc.perform(put(positionsUrl + "/" + positionId + "/roles")
			.with(administrator("can_position_assign_roles"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"roleCodes\":[\"" + foreignRoleCode + "\"]}"))
			.andExpect(status().isBadRequest());

		mockMvc.perform(put(positionsUrl + "/" + positionId + "/roles")
			.with(administrator("can_position_assign_roles"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"roleCodes\":[\"ADMIN\"]}"))
			.andExpect(status().isBadRequest());

		mockMvc.perform(put(positionsUrl + "/" + positionId + "/roles")
			.with(administrator("can_position_assign_roles"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"roleCodes\":[\"MISSING_ROLE\"]}"))
			.andExpect(status().isBadRequest());

		mockMvc.perform(put(positionsUrl + "/" + positionId + "/roles")
			.with(administrator("can_position_assign_roles"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"roleCodes\":[\"" + globalAccessRoleCode + "\"]}"))
			.andExpect(status().isBadRequest());
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor administrator(String permission) {
		return user("admin-tech").authorities(new SimpleGrantedAuthority("ADMIN_ACCESS"),
			new SimpleGrantedAuthority(permission));
	}
}