package com.galactique.gestionecole.enrollments.api;

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

import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;
import com.galactique.gestionecole.support.LicensedEstablishmentFixture;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class EnrollmentControllerTests {
	private final MockMvc mockMvc;
	private final EstablishmentRepository establishmentRepository;
	private final LicenseApplicationService licenseApplicationService;

	@Autowired
	EnrollmentControllerTests(MockMvc mockMvc, EstablishmentRepository establishmentRepository,
		LicenseApplicationService licenseApplicationService) {
		this.mockMvc = mockMvc;
		this.establishmentRepository = establishmentRepository;
		this.licenseApplicationService = licenseApplicationService;
	}

	@Test
	void enrollsStudentOncePerYearAndKeepsPreviousYearEnrollment() throws Exception {
		Establishment establishment = createEstablishment();
		String establishmentUrl = "/api/v1/establishments/" + establishment.id();
		String studentResponse = mockMvc.perform(post(establishmentUrl + "/students")
			.with(administrator("can_student_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"}"))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String studentId = JsonPath.read(studentResponse, "$.id");
		String firstYearId = createYear(establishmentUrl, "2025-2026");
		String firstClassId = createClass(establishmentUrl, firstYearId, "P1-A");
		String firstYearEnrollmentsUrl = enrollmentUrl(establishmentUrl, firstYearId);
		String enrollmentRequest = enrollmentRequest(studentId, firstClassId);

		mockMvc.perform(post(firstYearEnrollmentsUrl)
			.with(administrator("can_enrollment_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content(enrollmentRequest))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.studentId").value(studentId))
			.andExpect(jsonPath("$.schoolClassId").value(firstClassId));
		mockMvc.perform(get(establishmentUrl + "/school-years/" + firstYearId + "/classes")
			.with(administrator("can_school_class_read")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].classroomCount").value(1))
			.andExpect(jsonPath("$[0].totalCapacity").value(40))
			.andExpect(jsonPath("$[0].enrolledStudentCount").value(1));

		mockMvc.perform(post(firstYearEnrollmentsUrl)
			.with(administrator("can_enrollment_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content(enrollmentRequest))
			.andExpect(status().isConflict());

		mockMvc.perform(get(firstYearEnrollmentsUrl).with(administrator("can_enrollment_read")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)));

		mockMvc.perform(get(establishmentUrl + "/school-years/" + firstYearId
			+ "/classes/" + firstClassId + "/students")
			.with(administrator("can_enrollment_read")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].id").value(studentId));

		String secondYearId = createYear(establishmentUrl, "2026-2027");
		String secondClassId = createClass(establishmentUrl, secondYearId, "P1-B");
		mockMvc.perform(post(enrollmentUrl(establishmentUrl, secondYearId))
			.with(administrator("can_enrollment_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content(enrollmentRequest(studentId, firstClassId)))
			.andExpect(status().isNotFound());

		mockMvc.perform(post(enrollmentUrl(establishmentUrl, secondYearId))
			.with(administrator("can_enrollment_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content(enrollmentRequest(studentId, secondClassId)))
			.andExpect(status().isCreated());

		mockMvc.perform(get(firstYearEnrollmentsUrl).with(administrator("can_enrollment_read")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].academicYearId").value(firstYearId));
	}

	@Test
	void staffWithoutEnrollmentPermissionCannotReadEnrollments() throws Exception {
		Establishment establishment = createEstablishment();
		mockMvc.perform(get(enrollmentUrl("/api/v1/establishments/" + establishment.id(),
			UUID.randomUUID().toString()))
			.with(user("teacher@example.org").authorities(new SimpleGrantedAuthority("STAFF_ACCESS"))))
			.andExpect(status().isForbidden());
	}

	@Test
	void enrollmentRequiresAClassroomForTheSelectedClass() throws Exception {
		Establishment establishment = createEstablishment();
		String establishmentUrl = "/api/v1/establishments/" + establishment.id();
		String studentResponse = mockMvc.perform(post(establishmentUrl + "/students")
			.with(administrator("can_student_create"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"}"))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		String studentId = JsonPath.read(studentResponse, "$.id");
		String yearId = createYear(establishmentUrl, "2026-2027");
		String classResponse = mockMvc.perform(post(establishmentUrl + "/school-years/" + yearId + "/classes")
			.with(administrator("can_school_class_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"EMPTY\",\"name\":\"No classroom\"}"))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		String schoolClassId = JsonPath.read(classResponse, "$.id");
		mockMvc.perform(post(enrollmentUrl(establishmentUrl, yearId))
			.with(administrator("can_enrollment_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content(enrollmentRequest(studentId, schoolClassId)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Cette opération est impossible : aucune salle de classe n'est associée à cette classe. Veuillez d'abord lui affecter au moins une salle."));
	}

	private String createYear(String establishmentUrl, String name) throws Exception {
		String response = mockMvc.perform(post(establishmentUrl + "/school-years")
			.with(administrator("can_academic_year_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"" + name + "\",\"startsOn\":\"2025-09-01\",\"endsOn\":\"2026-06-30\"}"))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(response, "$.id");
	}

	private String createClass(String establishmentUrl, String academicYearId, String code) throws Exception {
		String response = mockMvc.perform(post(establishmentUrl + "/school-years/" + academicYearId + "/classes")
			.with(administrator("can_school_class_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"" + code + "\",\"name\":\"Class " + code + "\"}"))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String schoolClassId = JsonPath.read(response, "$.id");
		mockMvc.perform(post(establishmentUrl + "/school-years/" + academicYearId + "/classes/" + schoolClassId + "/classrooms")
			.with(administrator("can_school_class_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"A\",\"name\":\"Class " + code + "\",\"maxStudents\":40}"))
			.andExpect(status().isCreated());
		return schoolClassId;
	}

	private Establishment createEstablishment() {
		String code = "ENR-" + UUID.randomUUID().toString().substring(0, 8);
		return LicensedEstablishmentFixture.create(establishmentRepository, licenseApplicationService,
			code, "Enrollment Test School");
	}

	private static String enrollmentUrl(String establishmentUrl, String academicYearId) {
		return establishmentUrl + "/school-years/" + academicYearId + "/enrollments";
	}

	private static String enrollmentRequest(String studentId, String schoolClassId) {
		return "{\"studentId\":\"" + studentId + "\",\"schoolClassId\":\"" + schoolClassId + "\"}";
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor administrator(String permission) {
		return user("admin-tech").authorities(
			new SimpleGrantedAuthority("ADMIN_ACCESS"), new SimpleGrantedAuthority(permission));
	}
}