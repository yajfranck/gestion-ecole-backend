package com.galactique.gestionecole.guardians.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.employees.application.CreateEmployeeCommand;
import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.employees.application.EmployeeApplicationService;
import com.galactique.gestionecole.employees.application.EmployeeInvitationMailer;
import com.galactique.gestionecole.employees.domain.Employee;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;
import com.galactique.gestionecole.positions.application.PositionApplicationService;
import com.galactique.gestionecole.security.application.CreateRoleCommand;
import com.galactique.gestionecole.security.application.EstablishmentRoleApplicationService;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = "management.health.mail.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class GuardianChildPortalControllerTests {
	private static final Pattern INVITATION_TOKEN = Pattern.compile("#token=([A-Za-z0-9_-]+)");
	private static final String PARENT_PASSWORD = "parent-portal-password";

	private final MockMvc mockMvc;
	private final EstablishmentRepository establishmentRepository;
	private final EstablishmentRoleApplicationService establishmentRoleApplicationService;
	private final PositionApplicationService positionApplicationService;
	private final EmployeeApplicationService employeeApplicationService;
	private final LicenseApplicationService licenseApplicationService;

	@MockitoBean
	private JavaMailSender mailSender;

	@MockitoBean
	private EmployeeInvitationMailer employeeInvitationMailer;

	@Autowired
	GuardianChildPortalControllerTests(MockMvc mockMvc, EstablishmentRepository establishmentRepository,
		EstablishmentRoleApplicationService establishmentRoleApplicationService,
		PositionApplicationService positionApplicationService, EmployeeApplicationService employeeApplicationService,
		LicenseApplicationService licenseApplicationService) {
		this.mockMvc = mockMvc;
		this.establishmentRepository = establishmentRepository;
		this.establishmentRoleApplicationService = establishmentRoleApplicationService;
		this.positionApplicationService = positionApplicationService;
		this.employeeApplicationService = employeeApplicationService;
		this.licenseApplicationService = licenseApplicationService;
	}

	@Test
	void linkedGuardianGetsChildSummaryFeesAndOnlyCommunicableAttendance() throws Exception {
		Establishment establishment = createEstablishment();
		String base = "/api/v1/establishments/" + establishment.id();
		String studentId = createStudent(base, "Ada");
		String unlinkedStudentId = createStudent(base, "Unlinked");
		String academicYearId = createAcademicYear(base);
		String schoolClassId = createClass(base, academicYearId);
		createEnrollment(base, academicYearId, studentId, schoolClassId);
		createEnrollment(base, academicYearId, unlinkedStudentId, schoolClassId);
		String attendanceWriterToken = createAttendanceWriterLogin(establishment);

		String parentEmail = "parent-" + UUID.randomUUID() + "@example.org";
		linkGuardian(establishment, studentId, parentEmail);
		String parentToken = acceptGuardianInvitationAndLogin(parentEmail);

		String feeTypeId = createFeeType(base);
		configureCashPayment(base);
		String feeId = createTuitionFee(base, academicYearId, feeTypeId, schoolClassId,
			"Tuition USD", "100", "USD");
		createTuitionFee(base, academicYearId, feeTypeId, schoolClassId, "Activity EUR", "30", "EUR");
		String chargeId = JsonPath.read(mockMvc.perform(get(base + "/students/" + studentId + "/tuition-fees")
			.with(admin("can_tuition_read")))
			.andReturn().getResponse().getContentAsString(), "$[0].id");
		mockMvc.perform(post(base + "/students/" + studentId + "/tuition-fees/" + chargeId + "/payments")
			.with(admin("can_payment_record"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"amount\":25,\"paymentMethodCode\":\"CASH\"}"))
			.andExpect(status().isCreated());

		mockMvc.perform(put(attendanceUrl(establishment.id(), academicYearId, schoolClassId, "2026-02-12"))
			.header("Authorization", bearer(attendanceWriterToken))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"entries\":[{\"studentId\":\"" + studentId
				+ "\",\"status\":\"ABSENT\",\"absenceJustified\":true,"
				+ "\"comment\":\"Internal note\",\"parentComment\":\"Maladie signalée\"}]}"))
			.andExpect(status().isOk());

		String portalBase = "/api/v1/guardians/me/children/" + studentId;
		mockMvc.perform(get(portalBase + "/summary?academicYearId=" + academicYearId)
			.header("Authorization", bearer(parentToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.studentNumber").value(matchesPattern("STU-[0-9]{8,}")))
			.andExpect(jsonPath("$.schoolClassId").value(schoolClassId))
			.andExpect(jsonPath("$.balances", hasSize(2)))
			.andExpect(jsonPath("$.balances[0].currency").value("EUR"))
			.andExpect(jsonPath("$.balances[1].currency").value("USD"))
			.andExpect(jsonPath("$.balances[1].amountPaid").value(25.00));

		mockMvc.perform(get(portalBase + "/school-years/" + academicYearId + "/tuition-fees")
			.header("Authorization", bearer(parentToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[*].studentId").doesNotExist());

		mockMvc.perform(get(portalBase + "/school-years/" + academicYearId + "/tuition-fees/"
			+ chargeId + "/payments?from=2026-10-01&to=2026-10-31")
			.header("Authorization", bearer(parentToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].amount").value(25.00))
			.andExpect(jsonPath("$[0].recordedBy").doesNotExist());

		mockMvc.perform(get(portalBase + "/school-years/" + academicYearId
			+ "/attendance?from=2026-02-01&to=2026-02-28")
			.header("Authorization", bearer(parentToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.records", hasSize(1)))
			.andExpect(jsonPath("$.records[0].status").value("ABSENT"))
			.andExpect(jsonPath("$.records[0].parentComment").value("Maladie signalée"))
			.andExpect(jsonPath("$.records[0].comment").doesNotExist())
			.andExpect(jsonPath("$.records[0].createdBy").doesNotExist());

		mockMvc.perform(get("/api/v1/guardians/me/children/" + unlinkedStudentId
			+ "/summary?academicYearId=" + academicYearId)
			.header("Authorization", bearer(parentToken)))
			.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/guardians/me/children/" + unlinkedStudentId
			+ "/school-years/" + academicYearId + "/attendance?from=2026-02-01&to=2026-02-28")
			.header("Authorization", bearer(parentToken)))
			.andExpect(status().isNotFound());
	}

	@Test
	void guardianListsAllActiveYearsOfTheirChildsSchoolUsingTheExistingEndpoint() throws Exception {
		Establishment school = createEstablishment();
		String base = "/api/v1/establishments/" + school.id();
		String studentId = createStudent(base, "School years child");
		String previousYearId = createAcademicYear(base, 2025);
		String currentYearId = createAcademicYear(base, 2026);
		String archivedYearId = createAcademicYear(base, 2024);
		String nextYearId = createAcademicYear(base, 2027);
		mockMvc.perform(delete(base + "/school-years/" + archivedYearId)
			.with(admin("can_academic_year_delete"))).andExpect(status().isNoContent());
		Establishment otherSchool = createEstablishment();
		String otherBase = "/api/v1/establishments/" + otherSchool.id();
		String otherChildId = createStudent(otherBase, "Other school child");
		String otherYearId = createAcademicYear(otherBase, 2028);

		String email = "school-years-" + UUID.randomUUID() + "@example.org";
		String guardianId = linkGuardian(school, studentId, email);
		String parentToken = acceptGuardianInvitationAndLogin(email);
		String url = schoolYearsUrl(school.id().toString());
		mockMvc.perform(get(url).header("Authorization", bearer(parentToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(3)))
			.andExpect(jsonPath("$[*].id", contains(nextYearId, currentYearId, previousYearId)))
			.andExpect(jsonPath("$[0].establishmentId").value(school.id().toString()))
			.andExpect(jsonPath("$[0].name").value("2027-2028"))
			.andExpect(jsonPath("$[0].startsOn").value("2027-01-01"))
			.andExpect(jsonPath("$[0].endsOn").value("2027-12-31"))
			.andExpect(jsonPath("$[0].enabled").value(true))
			.andExpect(jsonPath("$[0].createdAt").exists());
		for (String forbiddenSchoolId : List.of(otherSchool.id().toString(), UUID.randomUUID().toString())) {
			mockMvc.perform(get(schoolYearsUrl(forbiddenSchoolId)).header("Authorization", bearer(parentToken)))
				.andExpect(status().isForbidden());
		}
		mockMvc.perform(get("/api/v1/guardians/me/children/" + studentId + "/school-years")
			.header("Authorization", bearer(parentToken))).andExpect(status().isNotFound());
		linkGuardian(otherSchool, otherChildId, email);
		mockMvc.perform(get(schoolYearsUrl(otherSchool.id().toString()))
			.header("Authorization", bearer(parentToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].id").value(otherYearId));
		mockMvc.perform(delete(base + "/students/" + studentId + "/guardians/" + guardianId)
			.with(admin("can_guardian_unlink"))).andExpect(status().isNoContent());
		mockMvc.perform(get(url).header("Authorization", bearer(parentToken)))
			.andExpect(status().isForbidden());
	}

	@Test
	void schoolYearsListGrantsNoManagementRightsAndRechecksLicenseAndChildStatus() throws Exception {
		Establishment school = createEstablishment();
		String base = "/api/v1/establishments/" + school.id();
		String studentId = createStudent(base, "Not yet enrolled child");
		String yearId = createAcademicYear(base);
		String email = "school-years-empty-" + UUID.randomUUID() + "@example.org";
		linkGuardian(school, studentId, email);
		String parentToken = acceptGuardianInvitationAndLogin(email);
		String url = schoolYearsUrl(school.id().toString());
		mockMvc.perform(get(url).header("Authorization", bearer(parentToken)))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
		mockMvc.perform(post(url).header("Authorization", bearer(parentToken))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"2028-2029\",\"startsOn\":\"2028-09-01\",\"endsOn\":\"2029-06-30\"}"))
			.andExpect(status().isForbidden());
		mockMvc.perform(patch(url + "/" + yearId).header("Authorization", bearer(parentToken))
			.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Changed\"}"))
			.andExpect(status().isForbidden());
		mockMvc.perform(delete(url + "/" + yearId).header("Authorization", bearer(parentToken)))
			.andExpect(status().isForbidden());
		mockMvc.perform(get(url + "/" + yearId).header("Authorization", bearer(parentToken)))
			.andExpect(status().isForbidden());
		mockMvc.perform(get(url + "/" + yearId + "/periods").header("Authorization", bearer(parentToken)))
			.andExpect(status().isForbidden());
		var license = licenseApplicationService.findLicenses(school.id(),
			new EmployeeActor("admin-tech", true, false)).getFirst();
		licenseApplicationService.suspend(school.id(), license.id(), "TEST_FIXTURE");
		mockMvc.perform(get(url).header("Authorization", bearer(parentToken)))
			.andExpect(status().isForbidden());
		licenseApplicationService.reactivate(school.id(), license.id(), "TEST_FIXTURE");
		mockMvc.perform(delete(base + "/students/" + studentId).with(admin("can_student_delete")))
			.andExpect(status().isNoContent());
		mockMvc.perform(get(url).header("Authorization", bearer(parentToken)))
			.andExpect(status().isForbidden());
	}

	@Test
	void schoolYearsListRequiresAuthenticationAndGuardianPermission() throws Exception {
		String url = schoolYearsUrl(UUID.randomUUID().toString());
		mockMvc.perform(get(url)).andExpect(status().isUnauthorized());
		mockMvc.perform(get(url).with(user("staff@example.org")
			.authorities(new SimpleGrantedAuthority("STAFF_ACCESS"))))
			.andExpect(status().isForbidden());
		mockMvc.perform(get(schoolYearsUrl("invalid-uuid")).with(user("parent@example.org")
			.authorities(new SimpleGrantedAuthority("can_guardian_read_children"))))
			.andExpect(status().isBadRequest());
	}

	private String linkGuardian(Establishment establishment, String studentId, String email) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/establishments/" + establishment.id() + "/students/" + studentId + "/guardians")
			.with(admin("can_guardian_link"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"firstName\":\"Grace\",\"lastName\":\"Guardian\",\"email\":\"" + email + "\"}"))
			.andExpect(status().isCreated()).andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.guardian.id");
	}

	private String createAttendanceWriterLogin(Establishment establishment) throws Exception {
		String email = "attendance-writer-" + UUID.randomUUID() + "@example.org";
		String roleCode = "ATTENDANCE_WRITE_" + UUID.randomUUID().toString().replace("-", "")
			.substring(0, 8).toUpperCase();
		EmployeeActor globalAdmin = new EmployeeActor("admin-tech", true, false);
		establishmentRoleApplicationService.createRole(establishment.id(), new CreateRoleCommand(roleCode,
			"Attendance Writer", null, List.of("can_attendance_write")), globalAdmin);
		var position = positionApplicationService.create(establishment.id(), "Attendance Writer",
			Set.of(roleCode), globalAdmin);
		Employee employee = employeeApplicationService.invite(new CreateEmployeeCommand(
			"ATT-" + UUID.randomUUID().toString().substring(0, 8), "Attendance", "Writer", email,
			position.id(), Set.of()), globalAdmin);
		ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
		org.mockito.Mockito.verify(employeeInvitationMailer).sendInvitation(org.mockito.ArgumentMatchers.eq(employee),
			token.capture());
		employeeApplicationService.acceptInvitation(token.getValue(), "attendance-writer-password");
		return login(email, "attendance-writer-password");
	}

	private String acceptGuardianInvitationAndLogin(String email) throws Exception {
		org.mockito.ArgumentCaptor<SimpleMailMessage> messageCaptor =
			org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
		org.mockito.Mockito.verify(mailSender).send(messageCaptor.capture());
		Matcher matcher = INVITATION_TOKEN.matcher(messageCaptor.getValue().getText());
		if (!matcher.find()) {
			throw new AssertionError("Guardian invitation did not contain a token");
		}
		mockMvc.perform(post("/api/v1/guardians/invitations/accept")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"token\":\"" + matcher.group(1) + "\",\"password\":\"" + PARENT_PASSWORD + "\"}"))
			.andExpect(status().isNoContent());
		return login(email, PARENT_PASSWORD);
	}

	private String login(String email, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"login\":\"" + email + "\",\"password\":\"" + password + "\"}"))
			.andExpect(status().isOk())
			.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
	}

	private String createStudent(String base, String firstName) throws Exception {
		MvcResult result = mockMvc.perform(post(base + "/students")
			.with(admin("can_student_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"firstName\":\"" + firstName + "\",\"lastName\":\"Portal\"}"))
			.andExpect(status().isCreated()).andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String createAcademicYear(String base) throws Exception {
		return createAcademicYear(base, 2026);
	}

	private String createAcademicYear(String base, int startsIn) throws Exception {
		MvcResult result = mockMvc.perform(post(base + "/school-years")
			.with(admin("can_academic_year_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"" + startsIn + "-" + (startsIn + 1) + "\",\"startsOn\":\""
				+ startsIn + "-01-01\",\"endsOn\":\"" + startsIn + "-12-31\"}"))
			.andExpect(status().isCreated()).andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String createClass(String base, String academicYearId) throws Exception {
		MvcResult result = mockMvc.perform(post(base + "/school-years/" + academicYearId + "/classes")
			.with(admin("can_school_class_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"PORTAL\",\"name\":\"Portal Class\"}"))
			.andExpect(status().isCreated()).andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private void createEnrollment(String base, String yearId, String studentId, String classId) throws Exception {
		mockMvc.perform(post(base + "/school-years/" + yearId + "/enrollments")
			.with(admin("can_enrollment_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"studentId\":\"" + studentId + "\",\"schoolClassId\":\"" + classId + "\"}"))
			.andExpect(status().isCreated());
	}

	private String createFeeType(String base) throws Exception {
		MvcResult result = mockMvc.perform(post(base + "/tuition-fee-types")
			.with(admin("can_tuition_fee_type_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"PORTAL_FEE\",\"name\":\"Portal fee\"}"))
			.andExpect(status().isCreated()).andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private void configureCashPayment(String base) throws Exception {
		mockMvc.perform(put(base + "/payment-methods")
			.with(admin("can_payment_method_update"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"methods\":[{\"code\":\"CASH\",\"name\":\"Cash\"}]}"))
			.andExpect(status().isOk());
	}

	private String createTuitionFee(String base, String yearId, String feeTypeId, String schoolClassId,
		String name, String amount, String currency) throws Exception {
		return mockMvc.perform(post(base + "/school-years/" + yearId + "/tuition-fees")
			.with(admin("can_tuition_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"" + name + "\",\"amount\":" + amount + ",\"currency\":\"" + currency
				+ "\",\"dueDate\":\"2026-03-31\",\"feeTypeId\":\"" + feeTypeId
				+ "\",\"schoolClassId\":\"" + schoolClassId + "\"}"))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
	}

	private Establishment createEstablishment() {
		Establishment establishment = establishmentRepository.save(
			"GCP-" + UUID.randomUUID().toString().substring(0, 8), "Guardian Portal School");
		licenseApplicationService.assign(establishment.id(),
			UUID.fromString("c51d6db7-309f-4e85-b884-000000000001"), null, null, "TEST_FIXTURE");
		return establishment;
	}

	private static String attendanceUrl(UUID establishmentId, String yearId, String classId, String date) {
		return "/api/v1/establishments/" + establishmentId + "/school-years/" + yearId
			+ "/classes/" + classId + "/attendance/" + date;
	}

	private static String schoolYearsUrl(String establishmentId) {
		return "/api/v1/establishments/" + establishmentId + "/school-years";
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor admin(String permission) {
		return user("admin-tech").authorities(new SimpleGrantedAuthority("ADMIN_ACCESS"),
			new SimpleGrantedAuthority(permission));
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}
}
