package com.galactique.gestionecole.tuition.api;

import static org.hamcrest.Matchers.hasSize;
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
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class TuitionControllerTests {
	private final MockMvc mockMvc;
	private final EstablishmentRepository establishmentRepository;

	@Autowired
	TuitionControllerTests(MockMvc mockMvc, EstablishmentRepository establishmentRepository) {
		this.mockMvc = mockMvc;
		this.establishmentRepository = establishmentRepository;
	}

	@Test
	void appliesFeesCollectsPartialPaymentsAndRetainsReceiptsAndBalances() throws Exception {
		Establishment establishment = establishmentRepository.save(
			"TUI-" + UUID.randomUUID().toString().substring(0, 8), "Tuition Test School");
		String base = "/api/v1/establishments/" + establishment.id();
		String studentId = createStudent(base);
		String gradeLevelId = createGradeLevel(base);
		String academicYearId = createAcademicYear(base);
		String schoolClassId = createClass(base, academicYearId, gradeLevelId);
		createEnrollment(base, academicYearId, studentId, schoolClassId);

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
				+ "\"dueDate\":\"2026-01-31\",\"gradeLevelId\":\"" + gradeLevelId + "\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.chargesCreated").value(1))
			.andReturn();
		String feeId = JsonPath.read(feeResult.getResponse().getContentAsString(), "$.fee.id");

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
		Establishment establishment = establishmentRepository.save(
			"TUP-" + UUID.randomUUID().toString().substring(0, 8), "Tuition Permission School");
		mockMvc.perform(get("/api/v1/establishments/" + establishment.id()
			+ "/school-years/" + UUID.randomUUID() + "/tuition-fees")
			.with(user("teacher@example.org").authorities(new SimpleGrantedAuthority("STAFF_ACCESS"))))
			.andExpect(status().isForbidden());
	}

	private String createStudent(String establishmentUrl) throws Exception {
		MvcResult result = mockMvc.perform(post(establishmentUrl + "/students")
			.with(admin("can_student_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"}"))
			.andExpect(status().isCreated()).andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String createGradeLevel(String establishmentUrl) throws Exception {
		MvcResult result = mockMvc.perform(post(establishmentUrl + "/grade-levels")
			.with(admin("can_grade_level_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"P1\",\"name\":\"Primary 1\",\"displayOrder\":1}"))
			.andExpect(status().isCreated()).andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
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

	private String createClass(String establishmentUrl, String yearId, String gradeLevelId) throws Exception {
		MvcResult result = mockMvc.perform(post(establishmentUrl + "/school-years/" + yearId + "/classes")
			.with(admin("can_school_class_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"gradeLevelId\":\"" + gradeLevelId
				+ "\",\"code\":\"P1-A\",\"name\":\"Primary 1 A\"}"))
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