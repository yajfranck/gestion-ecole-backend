package com.galactique.gestionecole.licenses.api;

import static org.hamcrest.Matchers.hasSize;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = "management.health.mail.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LicenseControllerTests {
	private final MockMvc mockMvc;
	private final EstablishmentRepository establishmentRepository;

	@Autowired
	LicenseControllerTests(MockMvc mockMvc, EstablishmentRepository establishmentRepository) {
		this.mockMvc = mockMvc;
		this.establishmentRepository = establishmentRepository;
	}

	@Test
	void adminCanAssignSuspendAndAuditAPlatformLicense() throws Exception {
		Establishment establishment = establishmentRepository.save(
			"LIC-" + UUID.randomUUID().toString().substring(0, 8), "License Test School");
		MvcResult offerResult = mockMvc.perform(post("/api/v1/admin/license-offers")
			.with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"TEST_" + UUID.randomUUID().toString().substring(0, 8)
				+ "\",\"name\":\"Test Offer\",\"features\":[\"LEARNING_CONTENT\"]}"))
			.andExpect(status().isCreated()).andReturn();
		String offerId = JsonPath.read(offerResult.getResponse().getContentAsString(), "$.id");

		MvcResult assignment = mockMvc.perform(post("/api/v1/admin/establishments/" + establishment.id() + "/licenses")
			.with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"offerId\":\"" + offerId + "\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.state").value("ACTIVE"))
			.andReturn();
		Object licenseIdValue = JsonPath.read(assignment.getResponse().getContentAsString(), "$.id");
		String licenseId = licenseIdValue.toString();

		mockMvc.perform(post("/api/v1/admin/establishments/" + establishment.id()
			+ "/licenses/" + licenseId + "/suspend").with(admin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.state").value("SUSPENDED"));
		mockMvc.perform(get("/api/v1/admin/establishments/" + establishment.id()
			+ "/licenses/" + licenseId + "/events").with(admin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)));
		mockMvc.perform(get("/api/v1/establishments/" + establishment.id() + "/license").with(admin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].state").value("SUSPENDED"));
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
		return user("admin-tech").authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
	}
}
