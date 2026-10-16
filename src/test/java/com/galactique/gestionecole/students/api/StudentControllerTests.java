package com.galactique.gestionecole.students.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;
import com.galactique.gestionecole.support.LicensedEstablishmentFixture;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class StudentControllerTests {
	private final MockMvc mockMvc;
	private final EstablishmentRepository establishmentRepository;
	private final LicenseApplicationService licenseApplicationService;

	@Autowired
	StudentControllerTests(MockMvc mockMvc, EstablishmentRepository establishmentRepository,
		LicenseApplicationService licenseApplicationService) {
		this.mockMvc = mockMvc;
		this.establishmentRepository = establishmentRepository;
		this.licenseApplicationService = licenseApplicationService;
	}

	@Test
	void administratorCanManageStudentRecordsWithinAnEstablishment() throws Exception {
		var establishment = createEstablishment();
		String createRequest = """
			{"firstName":" Ada ","lastName":" Lovelace "}
			""";

		String createResponse = mockMvc.perform(post(studentUrl(establishment.id()))
			.with(administrator("can_student_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content(createRequest))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.studentNumber").value(matchesPattern("STU-[0-9]{8,}")))
			.andExpect(jsonPath("$.firstName").value("Ada"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String studentId = JsonPath.read(createResponse, "$.id");
		String firstStudentNumber = JsonPath.read(createResponse, "$.studentNumber");

		String secondStudentResponse = mockMvc.perform(post(studentUrl(establishment.id()))
			.with(administrator("can_student_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"firstName\":\"Augusta\",\"lastName\":\"King\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.studentNumber").value(matchesPattern("STU-[0-9]{8,}")))
			.andReturn().getResponse().getContentAsString();
		assertNotEquals(firstStudentNumber, JsonPath.read(secondStudentResponse, "$.studentNumber"));

		mockMvc.perform(get(studentUrl(establishment.id()))
			.with(administrator("can_student_read")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)));

		mockMvc.perform(get(studentUrl(establishment.id()) + "/" + studentId)
			.with(administrator("can_student_read")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(studentId));

		mockMvc.perform(patch(studentUrl(establishment.id()) + "/" + studentId)
			.with(administrator("can_student_update"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"firstName\":\"Augusta\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.firstName").value("Augusta"));

		mockMvc.perform(delete(studentUrl(establishment.id()) + "/" + studentId)
			.with(administrator("can_student_delete")))
			.andExpect(status().isNoContent());

		mockMvc.perform(get(studentUrl(establishment.id()) + "/" + studentId)
			.with(administrator("can_student_read")))
			.andExpect(status().isNotFound());
	}

	@Test
	void staffWithoutStudentPermissionCannotReadStudentRecords() throws Exception {
		var establishment = createEstablishment();

		mockMvc.perform(get(studentUrl(establishment.id()))
			.with(user("teacher@example.org").authorities(new SimpleGrantedAuthority("STAFF_ACCESS"))))
			.andExpect(status().isForbidden());
	}

	private Establishment createEstablishment() {
		String code = "STU-" + UUID.randomUUID().toString().substring(0, 8);
		return LicensedEstablishmentFixture.create(establishmentRepository, licenseApplicationService,
			code, "Student Test School");
	}

	private static String studentUrl(UUID establishmentId) {
		return "/api/v1/establishments/" + establishmentId + "/students";
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor administrator(String permission) {
		return user("admin-tech").authorities(
			new SimpleGrantedAuthority("ADMIN_ACCESS"), new SimpleGrantedAuthority(permission));
	}
}