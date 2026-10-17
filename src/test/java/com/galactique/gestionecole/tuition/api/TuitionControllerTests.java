package com.galactique.gestionecole.tuition.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.matchesPattern;
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
import org.springframework.test.web.servlet.MvcResult;

import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;
import com.galactique.gestionecole.support.LicensedEstablishmentFixture;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class TuitionControllerTests {
	private final MockMvc mockMvc;
	private final EstablishmentRepository establishmentRepository;
	private final LicenseApplicationService licenseApplicationService;

	@Autowired
	TuitionControllerTests(MockMvc mockMvc, EstablishmentRepository establishmentRepository,
		LicenseApplicationService licenseApplicationService) {
		this.mockMvc = mockMvc;
		this.establishmentRepository = establishmentRepository;
		this.licenseApplicationService = licenseApplicationService;
	}

	@Test
	void appliesFeesCollectsPartialPaymentsAndRetainsReceiptsAndBalances() throws Exception {
		Establishment establishment = LicensedEstablishmentFixture.create(establishmentRepository,
			licenseApplicationService, "TUI-" + UUID.randomUUID().toString().substring(0, 8), "Tuition Test School");
		String base = "/api/v1/establishments/" + establishment.id();
		String studentId = createStudent(base);
		String otherStudentId = createStudent(base);
		String academicYearId = createAcademicYear(base);
		String schoolClassId = createClass(base, academicYearId, "P1-A");
		String otherClassId = createClass(base, academicYearId, "P1-B");
		createEnrollment(base, academicYearId, studentId, schoolClassId);
		createEnrollment(base, academicYearId, otherStudentId, otherClassId);

		mockMvc.perform(put(base + "/payment-methods")
			.with(admin("can_payment_method_update"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"methods\":[{\"code\":\"CASH\",\"name\":\"Cash\"}]}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)));
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
			base + "/payment-methods/CASH")
			.with(admin("can_payment_method_update"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"Cash desk\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Cash desk"));

		String feeTypeId = createFeeType(base);

		MvcResult feeResult = mockMvc.perform(post(base + "/school-years/" + academicYearId + "/tuition-fees")
			.with(admin("can_tuition_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"Tuition\",\"amount\":100.00,\"currency\":\"USD\","
				+ "\"feeTypeId\":\"" + feeTypeId + "\","
				+ "\"dueDate\":\"2026-01-31\",\"schoolClassId\":\"" + schoolClassId + "\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.chargesCreated").value(1))
			.andReturn();
		String feeId = JsonPath.read(feeResult.getResponse().getContentAsString(), "$.fee.id");
		assertClassFeeScope(feeResult, base, schoolClassId, otherStudentId);
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
			base + "/tuition-fee-types/" + feeTypeId)
			.with(admin("can_tuition_fee_type_update"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"Renamed Tuition Type\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Renamed Tuition Type"));
		mockMvc.perform(get(base + "/school-years/" + academicYearId + "/tuition-fees")
			.with(admin("can_tuition_read")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].feeTypeName").value("Tuition"));

		String chargesUrl = base + "/students/" + studentId + "/tuition-fees";
		MvcResult charges = mockMvc.perform(get(chargesUrl).with(admin("can_tuition_read")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].balance").value(100.00))
			.andReturn();
		String chargeId = JsonPath.read(charges.getResponse().getContentAsString(), "$[0].id");

		MvcResult firstPayment = recordPayment(base, studentId, chargeId, "30.00");
		mockMvc.perform(post(base + "/students/" + studentId + "/tuition-fees/" + chargeId + "/payments")
			.with(admin("can_payment_record"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"amount\":20.00,\"paymentMethodCode\":\"CASH\",\"reference\":\"cash-002\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.receiptNumber").isNotEmpty());

		String firstPaymentId = JsonPath.read(firstPayment.getResponse().getContentAsString(), "$.id");
		String firstReceiptNumber = JsonPath.read(firstPayment.getResponse().getContentAsString(), "$.receiptNumber");
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
			base + "/students/" + studentId)
			.with(admin("can_student_update"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"firstName\":\"Augusta\"}"))
			.andExpect(status().isOk());
		mockMvc.perform(get(base + "/payments/" + firstPaymentId + "/receipt")
			.with(admin("can_payment_receipt_read")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.receiptNumber").value(firstReceiptNumber))
			.andExpect(jsonPath("$.studentNumber").value(matchesPattern("STU-[0-9]{8,}")))
			.andExpect(jsonPath("$.studentFirstName").value("Ada"))
			.andExpect(jsonPath("$.paymentMethodName").value("Cash desk"))
			.andExpect(jsonPath("$.balanceAfterPayment").value(70.00));

		mockMvc.perform(get(chargesUrl).with(admin("can_tuition_read")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].totalPaid").value(50.00))
			.andExpect(jsonPath("$[0].balance").value(50.00))
			.andExpect(jsonPath("$[0].overdue").value(true))
			.andExpect(jsonPath("$[0].payments", hasSize(2)));

		mockMvc.perform(get(base + "/school-years/" + academicYearId + "/tuition-fees/outstanding")
			.with(admin("can_tuition_outstanding_read")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].balance").value(50.00));

		mockMvc.perform(post(base + "/students/" + studentId + "/tuition-fees/" + chargeId + "/payments")
			.with(admin("can_payment_record"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"amount\":50.01,\"paymentMethodCode\":\"CASH\"}"))
			.andExpect(status().isConflict());

		mockMvc.perform(post(base + "/school-years/" + academicYearId + "/tuition-fees/" + feeId + "/apply")
			.with(admin("can_tuition_apply")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.chargesCreated").value(0));

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
			base + "/payment-methods/CASH").with(admin("can_payment_method_delete")))
			.andExpect(status().isNoContent());
		mockMvc.perform(get(base + "/payment-methods").with(admin("can_tuition_read")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(0)));

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
			base + "/tuition-fee-types/" + feeTypeId).with(admin("can_tuition_fee_type_delete")))
			.andExpect(status().isNoContent());
		mockMvc.perform(get(base + "/tuition-fee-types").with(admin("can_tuition_fee_type_read")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void staffWithoutTuitionPermissionsCannotReadFees() throws Exception {
		Establishment establishment = LicensedEstablishmentFixture.create(establishmentRepository,
			licenseApplicationService, "TUP-" + UUID.randomUUID().toString().substring(0, 8), "Tuition Permission School");
		mockMvc.perform(get("/api/v1/establishments/" + establishment.id()
			+ "/school-years/" + UUID.randomUUID() + "/tuition-fees")
			.with(user("teacher@example.org").authorities(new SimpleGrantedAuthority("STAFF_ACCESS"))))
			.andExpect(status().isForbidden());
	}

	@Test
	void oneFeeTargetsMultipleClassesAndFutureEnrollmentsWithoutChargingOtherClasses() throws Exception {
		Establishment school = createEstablishment();
		String base = "/api/v1/establishments/" + school.id();
		String yearId = createAcademicYear(base);
		String firstClassId = createClass(base, yearId, "MULTI-A");
		String secondClassId = createClass(base, yearId, "MULTI-B");
		String excludedClassId = createClass(base, yearId, "EXCLUDED");
		String firstStudentId = createStudent(base);
		String secondStudentId = createStudent(base);
		String excludedStudentId = createStudent(base);
		createEnrollment(base, yearId, firstStudentId, firstClassId);
		createEnrollment(base, yearId, secondStudentId, secondClassId);
		createEnrollment(base, yearId, excludedStudentId, excludedClassId);
		String feeTypeId = createFeeType(base);
		String feesUrl = base + "/school-years/" + yearId + "/tuition-fees";
		MvcResult result = mockMvc.perform(post(feesUrl).with(admin("can_tuition_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content(feeRequest(feeTypeId, "\"schoolClassIds\":[\"" + firstClassId + "\",\"" + secondClassId + "\"]")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.chargesCreated").value(2))
			.andExpect(jsonPath("$.fee.scope").value("SCHOOL_CLASS"))
			.andExpect(jsonPath("$.fee.schoolClassId").isEmpty())
			.andExpect(jsonPath("$.fee.schoolClassIds", containsInAnyOrder(firstClassId, secondClassId)))
			.andReturn();
		String feeId = JsonPath.read(result.getResponse().getContentAsString(), "$.fee.id");
		mockMvc.perform(get(feesUrl).with(admin("can_tuition_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].schoolClassIds", containsInAnyOrder(firstClassId, secondClassId)));
		String newStudentId = createStudent(base);
		mockMvc.perform(put(base + "/payment-methods").with(admin("can_payment_method_update"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"methods\":[{\"code\":\"CASH\",\"name\":\"Cash\"}]}"))
			.andExpect(status().isOk());
		mockMvc.perform(post(base + "/school-years/" + yearId + "/enrollments")
			.with(user("admin-tech").authorities(new SimpleGrantedAuthority("ADMIN_ACCESS"),
				new SimpleGrantedAuthority("can_enrollment_create"), new SimpleGrantedAuthority("can_payment_record")))
			.contentType(MediaType.APPLICATION_JSON).content("{\"studentId\":\"" + newStudentId
				+ "\",\"schoolClassId\":\"" + secondClassId + "\",\"acceptAdvance\":true,\"payments\":[{\"feeId\":\""
				+ feeId + "\",\"amount\":10,\"paymentMethodCode\":\"CASH\"}]}"))
			.andExpect(status().isCreated());
		String newExcludedStudentId = createStudent(base);
		createEnrollment(base, yearId, newExcludedStudentId, excludedClassId);
		for (String studentId : java.util.List.of(firstStudentId, secondStudentId, newStudentId)) {
			mockMvc.perform(get(base + "/students/" + studentId + "/tuition-fees").with(admin("can_tuition_read")))
				.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].feeId").value(feeId))
				.andExpect(jsonPath("$[0].amount").value(100.00));
		}
		for (String studentId : java.util.List.of(excludedStudentId, newExcludedStudentId)) {
			mockMvc.perform(get(base + "/students/" + studentId + "/tuition-fees").with(admin("can_tuition_read")))
				.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
		}
		mockMvc.perform(post(feesUrl + "/" + feeId + "/apply").with(admin("can_tuition_apply")))
			.andExpect(status().isOk()).andExpect(jsonPath("$.chargesCreated").value(0));
	}

	@Test
	void invalidClassSelectionsDoNotCreateFeesOrCharges() throws Exception {
		Establishment school = createEstablishment();
		String base = "/api/v1/establishments/" + school.id();
		String yearId = createAcademicYear(base);
		String classId = createClass(base, yearId, "VALIDATION");
		String studentId = createStudent(base);
		createEnrollment(base, yearId, studentId, classId);
		String feeTypeId = createFeeType(base);
		String feesUrl = base + "/school-years/" + yearId + "/tuition-fees";
		for (String selection : java.util.List.of(
			"\"schoolClassIds\":[]",
			"\"schoolClassIds\":[null]",
			"\"schoolClassIds\":[\"" + classId + "\",\"" + classId + "\"]",
			"\"schoolClassId\":\"" + classId + "\",\"schoolClassIds\":[\"" + classId + "\"]",
			"\"studentId\":\"" + studentId + "\",\"schoolClassIds\":[\"" + classId + "\"]")) {
			mockMvc.perform(post(feesUrl).with(admin("can_tuition_create"))
				.contentType(MediaType.APPLICATION_JSON).content(feeRequest(feeTypeId, selection)))
				.andExpect(status().isBadRequest());
		}
		mockMvc.perform(get(feesUrl).with(admin("can_tuition_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
		mockMvc.perform(get(base + "/students/" + studentId + "/tuition-fees").with(admin("can_tuition_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void allSelectedClassesMustBeActiveAndBelongToTheRequestedSchoolAndYear() throws Exception {
		Establishment school = createEstablishment();
		String base = "/api/v1/establishments/" + school.id();
		String yearId = createAcademicYear(base);
		String validClassId = createClass(base, yearId, "VALID");
		String studentId = createStudent(base);
		createEnrollment(base, yearId, studentId, validClassId);
		String archivedClassId = createClass(base, yearId, "ARCHIVED");
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
			base + "/school-years/" + yearId + "/classes/" + archivedClassId)
			.with(admin("can_school_class_delete"))).andExpect(status().isNoContent());
		String otherYearResponse = mockMvc.perform(post(base + "/school-years")
			.with(admin("can_academic_year_create")).contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"2026-2027\",\"startsOn\":\"2026-09-01\",\"endsOn\":\"2027-06-30\"}"))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		String otherYearClassId = createClass(base, JsonPath.read(otherYearResponse, "$.id"), "OTHER-YEAR");
		Establishment otherSchool = createEstablishment();
		String otherBase = "/api/v1/establishments/" + otherSchool.id();
		String foreignClassId = createClass(otherBase, createAcademicYear(otherBase), "FOREIGN");
		String feeTypeId = createFeeType(base);
		String feesUrl = base + "/school-years/" + yearId + "/tuition-fees";
		for (String invalidClassId : java.util.List.of(archivedClassId, otherYearClassId, foreignClassId, UUID.randomUUID().toString())) {
			mockMvc.perform(post(feesUrl).with(admin("can_tuition_create"))
				.contentType(MediaType.APPLICATION_JSON).content(feeRequest(feeTypeId,
					"\"schoolClassIds\":[\"" + validClassId + "\",\"" + invalidClassId + "\"]")))
				.andExpect(status().isNotFound());
		}
		mockMvc.perform(get(feesUrl).with(admin("can_tuition_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
		mockMvc.perform(get(base + "/students/" + studentId + "/tuition-fees").with(admin("can_tuition_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
	}

	private Establishment createEstablishment() {
		return LicensedEstablishmentFixture.create(establishmentRepository, licenseApplicationService,
			"TUI-" + UUID.randomUUID().toString().substring(0, 8), "Tuition Test School");
	}

	private static String feeRequest(String feeTypeId, String selection) {
		return "{\"name\":\"Multi-class tuition\",\"feeTypeId\":\"" + feeTypeId
			+ "\",\"amount\":100.00,\"currency\":\"USD\",\"dueDate\":\"2026-01-31\"," + selection + "}";
	}

	private String createStudent(String establishmentUrl) throws Exception {
		MvcResult result = mockMvc.perform(post(establishmentUrl + "/students")
			.with(admin("can_student_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"}"))
			.andExpect(status().isCreated()).andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private void assertClassFeeScope(MvcResult feeResult, String establishmentUrl,
		String schoolClassId, String otherStudentId) throws Exception {
		String response = feeResult.getResponse().getContentAsString();
		org.junit.jupiter.api.Assertions.assertEquals("SCHOOL_CLASS", JsonPath.read(response, "$.fee.scope"));
		org.junit.jupiter.api.Assertions.assertEquals(schoolClassId, JsonPath.read(response, "$.fee.schoolClassId"));
		org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of(schoolClassId), JsonPath.read(response, "$.fee.schoolClassIds"));
		mockMvc.perform(get(establishmentUrl + "/students/" + otherStudentId + "/tuition-fees")
			.with(admin("can_tuition_read")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(0)));
	}

	private String createFeeType(String establishmentUrl) throws Exception {
		MvcResult response = mockMvc.perform(post(establishmentUrl + "/tuition-fee-types")
			.with(admin("can_tuition_fee_type_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"TUITION\",\"name\":\"Tuition\"}"))
			.andExpect(status().isCreated())
			.andReturn();
		return JsonPath.read(response.getResponse().getContentAsString(), "$.id");
	}

	private String createAcademicYear(String establishmentUrl) throws Exception {
		MvcResult result = mockMvc.perform(post(establishmentUrl + "/school-years")
			.with(admin("can_academic_year_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"2025-2026\",\"startsOn\":\"2025-09-01\",\"endsOn\":\"2026-06-30\"}"))
			.andExpect(status().isCreated()).andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String createClass(String establishmentUrl, String yearId, String code) throws Exception {
		MvcResult result = mockMvc.perform(post(establishmentUrl + "/school-years/" + yearId + "/classes")
			.with(admin("can_school_class_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"" + code + "\",\"name\":\"Class " + code + "\"}"))
			.andExpect(status().isCreated()).andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private void createEnrollment(String establishmentUrl, String yearId, String studentId, String schoolClassId)
		throws Exception {
		mockMvc.perform(post(establishmentUrl + "/school-years/" + yearId + "/enrollments")
			.with(admin("can_enrollment_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"studentId\":\"" + studentId + "\",\"schoolClassId\":\"" + schoolClassId + "\"}"))
			.andExpect(status().isCreated());
	}

	private MvcResult recordPayment(String establishmentUrl, String studentId, String chargeId, String amount)
		throws Exception {
		return mockMvc.perform(post(establishmentUrl + "/students/" + studentId
			+ "/tuition-fees/" + chargeId + "/payments")
			.with(admin("can_payment_record"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"amount\":" + amount + ",\"paymentMethodCode\":\"CASH\",\"reference\":\"cash-001\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.receiptNumber").isNotEmpty())
			.andReturn();
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor admin(String permission) {
		return user("admin-tech").authorities(new SimpleGrantedAuthority("ADMIN_ACCESS"),
			new SimpleGrantedAuthority(permission));
	}
}
