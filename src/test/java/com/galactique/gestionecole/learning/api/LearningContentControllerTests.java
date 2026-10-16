package com.galactique.gestionecole.learning.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

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
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;

import org.junit.jupiter.api.Test;

@SpringBootTest(properties = "management.health.mail.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LearningContentControllerTests {
	private final MockMvc mockMvc;
	private final EstablishmentRepository establishmentRepository;
	private final LicenseApplicationService licenseApplicationService;

	@Autowired
	LearningContentControllerTests(MockMvc mockMvc, EstablishmentRepository establishmentRepository,
		LicenseApplicationService licenseApplicationService) {
		this.mockMvc = mockMvc;
		this.establishmentRepository = establishmentRepository;
		this.licenseApplicationService = licenseApplicationService;
	}

	@Test
	void contentMustBePublishedBeforeItCanBeReturnedByPublishedRoute() throws Exception {
		Establishment establishment = establishmentRepository.save(
			"LRN-" + UUID.randomUUID().toString().substring(0, 8), "Learning Test School");
		licenseApplicationService.assign(establishment.id(),
			UUID.fromString("c51d6db7-309f-4e85-b884-000000000001"), null, null, "TEST_FIXTURE");
		String base = "/api/v1/establishments/" + establishment.id();
		String yearId = createAcademicYear(base);
		String classId = createClass(base, yearId);
		String learningBase = base + "/school-years/" + yearId + "/classes/" + classId + "/learning-content";
		MvcResult created = mockMvc.perform(post(learningBase)
			.with(admin("can_learning_content_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"type\":\"LESSON\",\"title\":\"Fractions\",\"description\":\"Unit 2\",\"body\":\"Equivalent fractions\"}"))
			.andExpect(status().isCreated()).andReturn();
		String contentId = com.jayway.jsonpath.JsonPath.read(
			created.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get(learningBase).with(admin("can_learning_content_read")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.contents", hasSize(1)))
			.andExpect(jsonPath("$.contents[0].status").value("DRAFT"));

		mockMvc.perform(post(learningBase + "/" + contentId + "/publish")
			.with(admin("can_learning_content_publish")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("PUBLISHED"));
	}

	private String createAcademicYear(String base) throws Exception {
		MvcResult result = mockMvc.perform(post(base + "/school-years")
			.with(admin("can_academic_year_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"2026-2027\",\"startsOn\":\"2026-01-01\",\"endsOn\":\"2026-12-31\"}"))
			.andExpect(status().isCreated()).andReturn();
		return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String createClass(String base, String academicYearId) throws Exception {
		MvcResult result = mockMvc.perform(post(base + "/school-years/" + academicYearId + "/classes")
			.with(admin("can_school_class_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"LEARNING\",\"name\":\"Learning Class\"}"))
			.andExpect(status().isCreated()).andReturn();
		return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor admin(String permission) {
		return user("admin-tech").authorities(new SimpleGrantedAuthority("ADMIN_ACCESS"),
			new SimpleGrantedAuthority(permission));
	}
}
