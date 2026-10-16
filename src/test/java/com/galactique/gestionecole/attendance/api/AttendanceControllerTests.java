package com.galactique.gestionecole.attendance.api;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.galactique.gestionecole.employees.application.CreateEmployeeCommand;
import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.employees.application.EmployeeApplicationService;
import com.galactique.gestionecole.employees.application.EmployeeInvitationMailer;
import com.galactique.gestionecole.employees.domain.Employee;
import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.positions.application.PositionApplicationService;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;
import com.galactique.gestionecole.support.LicensedEstablishmentFixture;
import com.galactique.gestionecole.security.application.CreateRoleCommand;
import com.galactique.gestionecole.security.application.EstablishmentRoleApplicationService;
import com.galactique.gestionecole.security.domain.UserAccountRepository;
import com.galactique.gestionecole.security.infrastructure.security.SecurityUserPrincipal;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = "management.health.mail.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AttendanceControllerTests {
	private static final String ADMIN_PASSWORD = "attendance-operator-password";
	private static final EmployeeActor GLOBAL_ADMIN = new EmployeeActor("admin-tech", true, false);
	private static final String ATTENDANCE_DATE = "2026-02-12";

	private final MockMvc mockMvc;
	private final EstablishmentRepository establishmentRepository;
	private final EstablishmentRoleApplicationService establishmentRoleApplicationService;
	private final PositionApplicationService positionApplicationService;
	private final EmployeeApplicationService employeeApplicationService;
	private final UserAccountRepository userAccountRepository;
	private final LicenseApplicationService licenseApplicationService;

	@MockitoBean
	private EmployeeInvitationMailer invitationMailer;

	@Autowired
	AttendanceControllerTests(MockMvc mockMvc, EstablishmentRepository establishmentRepository,
		EstablishmentRoleApplicationService establishmentRoleApplicationService,
		PositionApplicationService positionApplicationService, EmployeeApplicationService employeeApplicationService,
		UserAccountRepository userAccountRepository, LicenseApplicationService licenseApplicationService) {
		this.mockMvc = mockMvc;
		this.establishmentRepository = establishmentRepository;
		this.establishmentRoleApplicationService = establishmentRoleApplicationService;
		this.positionApplicationService = positionApplicationService;
		this.employeeApplicationService = employeeApplicationService;
		this.userAccountRepository = userAccountRepository;
		this.licenseApplicationService = licenseApplicationService;
	}

	@Test
	void recordsCorrectsAndReadsAttendanceHistoryAfterStudentAndClassArchival() throws Exception {
		AttendanceFixture fixture = createFixture();
		String url = attendanceSheetUrl(fixture, ATTENDANCE_DATE);

		MvcResult emptySheet = mockMvc.perform(get(url).header("Authorization", bearer(fixture.accessToken())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.presentCount").value(0))
			.andExpect(jsonPath("$.absentCount").value(0))
			.andExpect(jsonPath("$.unreportedCount").value(2))
			.andExpect(jsonPath("$.entries[*].status", containsInAnyOrder("NON_RENSEIGNE", "NON_RENSEIGNE")))
			.andReturn();
		assertNotNull(emptySheet.getResponse().getContentAsString());

		String initialRequest = batch(entry(fixture.firstStudentId(), "PRESENT", false, null),
			entry(fixture.secondStudentId(), "ABSENT", true, "Fièvre", "Maladie signalée", null));
		MvcResult initial = mockMvc.perform(put(attendanceWriteUrl(fixture, ATTENDANCE_DATE))
			.header("Authorization", bearer(fixture.accessToken()))
			.contentType(MediaType.APPLICATION_JSON)
			.content(initialRequest))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.presentCount").value(1))
			.andExpect(jsonPath("$.absentCount").value(1))
			.andExpect(jsonPath("$.unreportedCount").value(0))
			.andReturn();
		Map<String, Object> originalRecord = entryFor(initial.getResponse().getContentAsString(), fixture.firstStudentId());

		MvcResult repeated = mockMvc.perform(put(attendanceWriteUrl(fixture, ATTENDANCE_DATE))
			.header("Authorization", bearer(fixture.accessToken()))
			.contentType(MediaType.APPLICATION_JSON)
			.content(initialRequest))
			.andExpect(status().isOk())
			.andReturn();
		Map<String, Object> repeatedRecord = entryFor(repeated.getResponse().getContentAsString(), fixture.firstStudentId());
		assertEquals(originalRecord.get("createdAt"), repeatedRecord.get("createdAt"));
		assertEquals(originalRecord.get("updatedAt"), repeatedRecord.get("updatedAt"));

		MvcResult correction = mockMvc.perform(put(attendanceWriteUrl(fixture, ATTENDANCE_DATE))
			.header("Authorization", bearer(fixture.accessToken()))
			.contentType(MediaType.APPLICATION_JSON)
			.content(batch(entry(fixture.firstStudentId(), "ABSENT", false, "Arrivé après l’appel", null,
				(String) originalRecord.get("updatedAt")))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.presentCount").value(0))
			.andExpect(jsonPath("$.absentCount").value(2))
			.andExpect(jsonPath("$.unreportedCount").value(0))
			.andReturn();
		Map<String, Object> unchangedOmittedEntry = entryFor(
			correction.getResponse().getContentAsString(), fixture.secondStudentId());
		assertEquals("Fièvre", unchangedOmittedEntry.get("comment"));
		mockMvc.perform(put(attendanceWriteUrl(fixture, ATTENDANCE_DATE))
			.header("Authorization", bearer(fixture.accessToken()))
			.contentType(MediaType.APPLICATION_JSON)
			.content(batch(entry(fixture.firstStudentId(), "PRESENT", false, null,
				(String) originalRecord.get("updatedAt")))))
			.andExpect(status().isConflict());

		mockMvc.perform(delete(studentUrl(fixture, fixture.firstStudentId()))
			.with(admin("can_student_delete")))
			.andExpect(status().isNoContent());
		mockMvc.perform(delete(classUrl(fixture))
			.with(admin("can_school_class_delete")))
			.andExpect(status().isNoContent());

		mockMvc.perform(get(studentHistoryUrl(fixture, fixture.firstStudentId(), "2026-02-01", "2026-02-28"))
			.header("Authorization", bearer(fixture.accessToken())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.records", org.hamcrest.Matchers.hasSize(1)))
			.andExpect(jsonPath("$.records[0].status").value("ABSENT"))
			.andExpect(jsonPath("$.records[0].comment").value("Arrivé après l’appel"))
			.andExpect(jsonPath("$.records[0].createdBy").value(fixture.staffEmail()))
			.andExpect(jsonPath("$.records[0].updatedBy").value(fixture.staffEmail()));
	}

	@Test
	void validatesWholeBatchDateAndScopeBeforeWriting() throws Exception {
		AttendanceFixture fixture = createFixture();
		String invalidDateUrl = attendanceSheetUrl(fixture, "2025-12-31");

		mockMvc.perform(get(invalidDateUrl).header("Authorization", bearer(fixture.accessToken())))
			.andExpect(status().isBadRequest());

		String nextDateUrl = attendanceSheetUrl(fixture, "2026-02-13");
		mockMvc.perform(put(attendanceWriteUrl(fixture, "2026-02-13"))
			.header("Authorization", bearer(fixture.accessToken()))
			.contentType(MediaType.APPLICATION_JSON)
			.content(batch(entry(fixture.firstStudentId(), "PRESENT", false, null),
				entry(UUID.randomUUID().toString(), "ABSENT", false, null))))
			.andExpect(status().isBadRequest());
		mockMvc.perform(get(nextDateUrl).header("Authorization", bearer(fixture.accessToken())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.unreportedCount").value(2));

		mockMvc.perform(put(attendanceWriteUrl(fixture, "2026-02-13"))
			.header("Authorization", bearer(fixture.accessToken()))
			.contentType(MediaType.APPLICATION_JSON)
			.content(batch(entry(fixture.firstStudentId(), "PRESENT", false, null),
				entry(fixture.firstStudentId(), "ABSENT", false, null))))
			.andExpect(status().isBadRequest());
		mockMvc.perform(put(attendanceWriteUrl(fixture, "2026-02-13"))
			.header("Authorization", bearer(fixture.accessToken()))
			.contentType(MediaType.APPLICATION_JSON)
			.content(batch(entry(fixture.firstStudentId(), "PRESENT", true, null))))
			.andExpect(status().isBadRequest());

		Establishment otherEstablishment = createEstablishment();
		mockMvc.perform(get("/api/v1/establishments/" + otherEstablishment.id()
			+ "/school-years/" + fixture.academicYearId() + "/classes/" + fixture.schoolClassId()
			+ "/attendance?date=2026-02-13")
			.header("Authorization", bearer(fixture.accessToken())))
			.andExpect(status().isForbidden());

		var globalAdmin = userAccountRepository.findByUsername("admin-tech").orElseThrow();
		mockMvc.perform(get(attendanceSheetUrl(fixture, "2026-02-13"))
			.with(user(new SecurityUserPrincipal(globalAdmin))))
			.andExpect(status().isForbidden());
	}

	private AttendanceFixture createFixture() throws Exception {
		Establishment establishment = createEstablishment();
		String base = "/api/v1/establishments/" + establishment.id();
		String firstStudentId = createStudent(base, "Ada");
		String secondStudentId = createStudent(base, "Grace");
		String academicYearId = createYear(base);
		String schoolClassId = createClass(base, academicYearId);
		createEnrollment(base, academicYearId, firstStudentId, schoolClassId);
		createEnrollment(base, academicYearId, secondStudentId, schoolClassId);
		String email = "attendance-" + UUID.randomUUID() + "@example.org";
		String roleCode = "ATTENDANCE_" + UUID.randomUUID().toString().replace("-", "")
			.substring(0, 8).toUpperCase();
		establishmentRoleApplicationService.createRole(establishment.id(), new CreateRoleCommand(roleCode,
			"Attendance Operator", null, List.of("can_attendance_read", "can_attendance_write")), GLOBAL_ADMIN);
		var position = positionApplicationService.create(establishment.id(), "Attendance Operator",
			Set.of(roleCode), GLOBAL_ADMIN);
		Employee employee = employeeApplicationService.invite(new CreateEmployeeCommand(
			"ATT-" + UUID.randomUUID().toString().substring(0, 8), "Attendance", "Operator", email,
			position.id(), Set.of()), GLOBAL_ADMIN);
		ArgumentCaptor<String> invitationToken = ArgumentCaptor.forClass(String.class);
		org.mockito.Mockito.verify(invitationMailer).sendInvitation(org.mockito.ArgumentMatchers.eq(employee),
			invitationToken.capture());
		employeeApplicationService.acceptInvitation(invitationToken.getValue(), ADMIN_PASSWORD);
		MvcResult login = mockMvc.perform(post("/api/v1/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"login\":\"" + email + "\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
			.andExpect(status().isOk())
			.andReturn();
		String token = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
		return new AttendanceFixture(establishment.id(), academicYearId, schoolClassId,
			firstStudentId, secondStudentId, email, token);
	}

	private String createStudent(String base, String firstName) throws Exception {
		MvcResult result = mockMvc.perform(post(base + "/students")
			.with(admin("can_student_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"firstName\":\"" + firstName + "\",\"lastName\":\"Attendance\"}"))
			.andExpect(status().isCreated())
			.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String createYear(String base) throws Exception {
		MvcResult result = mockMvc.perform(post(base + "/school-years")
			.with(admin("can_academic_year_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"2026-2027\",\"startsOn\":\"2026-01-01\",\"endsOn\":\"2026-12-31\"}"))
			.andExpect(status().isCreated())
			.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String createClass(String base, String academicYearId) throws Exception {
		MvcResult result = mockMvc.perform(post(base + "/school-years/" + academicYearId + "/classes")
			.with(admin("can_school_class_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"A\",\"name\":\"Class A\"}"))
			.andExpect(status().isCreated())
			.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private void createEnrollment(String base, String academicYearId, String studentId, String schoolClassId)
		throws Exception {
		mockMvc.perform(post(base + "/school-years/" + academicYearId + "/enrollments")
			.with(admin("can_enrollment_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"studentId\":\"" + studentId + "\",\"schoolClassId\":\"" + schoolClassId + "\"}"))
			.andExpect(status().isCreated());
	}

	private Establishment createEstablishment() {
		return LicensedEstablishmentFixture.create(establishmentRepository, licenseApplicationService,
			"ATT-" + UUID.randomUUID().toString().substring(0, 8), "Attendance School");
	}

	private String attendanceSheetUrl(AttendanceFixture fixture, String date) {
		return "/api/v1/establishments/" + fixture.establishmentId() + "/school-years/"
			+ fixture.academicYearId() + "/classes/" + fixture.schoolClassId() + "/attendance?date=" + date;
	}

	private String attendanceWriteUrl(AttendanceFixture fixture, String date) {
		return "/api/v1/establishments/" + fixture.establishmentId() + "/school-years/"
			+ fixture.academicYearId() + "/classes/" + fixture.schoolClassId() + "/attendance/" + date;
	}

	private String classUrl(AttendanceFixture fixture) {
		return "/api/v1/establishments/" + fixture.establishmentId() + "/school-years/"
			+ fixture.academicYearId() + "/classes/" + fixture.schoolClassId();
	}

	private String studentUrl(AttendanceFixture fixture, String studentId) {
		return "/api/v1/establishments/" + fixture.establishmentId() + "/students/" + studentId;
	}

	private String studentHistoryUrl(AttendanceFixture fixture, String studentId, String from, String to) {
		return "/api/v1/establishments/" + fixture.establishmentId() + "/school-years/"
			+ fixture.academicYearId() + "/students/" + studentId + "/attendance?from=" + from + "&to=" + to;
	}

	private static String batch(String... entries) {
		return "{\"entries\":[" + String.join(",", entries) + "]}";
	}

	private static String entry(String studentId, String status, boolean justified, String comment) {
		return entry(studentId, status, justified, comment, null);
	}

	private static String entry(String studentId, String status, boolean justified,
		String comment, String expectedUpdatedAt) {
		return entry(studentId, status, justified, comment, null, expectedUpdatedAt);
	}

	private static String entry(String studentId, String status, boolean justified,
		String comment, String parentComment, String expectedUpdatedAt) {
		String commentJson = comment == null ? "null" : "\"" + comment + "\"";
		String parentCommentJson = parentComment == null ? "null" : "\"" + parentComment + "\"";
		return "{\"studentId\":\"" + studentId + "\",\"status\":\"" + status
			+ "\",\"absenceJustified\":" + justified + ",\"comment\":" + commentJson
			+ ",\"parentComment\":" + parentCommentJson
			+ ",\"expectedUpdatedAt\":"
			+ (expectedUpdatedAt == null ? "null" : "\"" + expectedUpdatedAt + "\"") + "}";
	}

	private Map<String, Object> entryFor(String response, String studentId) {
		List<Map<String, Object>> entries = JsonPath.read(response, "$.entries");
		return entries.stream().filter(entry -> studentId.equals(entry.get("studentId"))).findFirst().orElseThrow();
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor admin(String permission) {
		return user("admin-tech").authorities(new SimpleGrantedAuthority("ADMIN_ACCESS"),
			new SimpleGrantedAuthority(permission));
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}

	private record AttendanceFixture(UUID establishmentId, String academicYearId, String schoolClassId,
		String firstStudentId, String secondStudentId, String staffEmail, String accessToken) {
	}
}
