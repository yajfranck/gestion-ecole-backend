package com.galactique.gestionecole.schoolclasses.api;

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

import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;
import com.galactique.gestionecole.support.LicensedEstablishmentFixture;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class SchoolCatalogControllerTests {
	private final MockMvc mockMvc;
	private final EstablishmentRepository establishmentRepository;
	private final LicenseApplicationService licenseApplicationService;

	@Autowired
	SchoolCatalogControllerTests(MockMvc mockMvc, EstablishmentRepository establishmentRepository,
		LicenseApplicationService licenseApplicationService) {
		this.mockMvc = mockMvc;
		this.establishmentRepository = establishmentRepository;
		this.licenseApplicationService = licenseApplicationService;
	}

	@Test
	void configuresClassesSubjectsAndClassSubjectAssignments() throws Exception {
		Establishment establishment = createEstablishment();
		String establishmentUrl = "/api/v1/establishments/" + establishment.id();
		String subjectResponse = mockMvc.perform(post(establishmentUrl + "/subjects")
			.with(administrator("can_subject_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"MATH\",\"name\":\"Mathematics\"}"))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String subjectId = JsonPath.read(subjectResponse, "$.id");
		mockMvc.perform(get(establishmentUrl + "/subjects/" + subjectId)
			.with(administrator("can_subject_read")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("MATH"));

		String yearResponse = mockMvc.perform(post(establishmentUrl + "/school-years")
			.with(administrator("can_academic_year_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"2026-2027\",\"startsOn\":\"2026-09-01\",\"endsOn\":\"2027-06-30\"}"))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String yearId = JsonPath.read(yearResponse, "$.id");
		String classesUrl = establishmentUrl + "/school-years/" + yearId + "/classes";

		String schoolClassResponse = mockMvc.perform(post(classesUrl)
			.with(administrator("can_school_class_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"P1-A\",\"name\":\"Primary 1 A\"}"))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String schoolClassId = JsonPath.read(schoolClassResponse, "$.id");
		mockMvc.perform(get(classesUrl + "/" + schoolClassId)
			.with(administrator("can_school_class_read")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("P1-A"))
			.andExpect(jsonPath("$.gradeLevelId").doesNotExist());

		mockMvc.perform(put(classesUrl + "/" + schoolClassId + "/subjects/" + subjectId)
			.with(administrator("can_class_subject_assign")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.enabled").value(true));

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
			.delete(establishmentUrl + "/subjects/" + subjectId)
			.with(administrator("can_subject_delete")))
			.andExpect(status().isConflict());

		mockMvc.perform(get(classesUrl + "/" + schoolClassId + "/subjects")
			.with(administrator("can_class_subject_read")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].subjectId").value(subjectId));
	}

	private Establishment createEstablishment() {
		String code = "CAT-" + UUID.randomUUID().toString().substring(0, 8);
		return LicensedEstablishmentFixture.create(establishmentRepository, licenseApplicationService,
			code, "Catalog Test School");
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor administrator(String permission) {
		return user("admin-tech").authorities(
			new SimpleGrantedAuthority("ADMIN_ACCESS"), new SimpleGrantedAuthority(permission));
	}
}