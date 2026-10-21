package com.galactique.gestionecole.enrollments.api;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;
import com.galactique.gestionecole.licenses.domain.LicenseFeature;
import com.galactique.gestionecole.schoolclasses.application.CreateClassroomCommand;
import com.galactique.gestionecole.schoolclasses.application.CreateSchoolClassCommand;
import com.galactique.gestionecole.schoolclasses.application.SchoolCatalogApplicationService;
import com.galactique.gestionecole.schoolclasses.domain.AcademicYearRepository;
import com.galactique.gestionecole.students.domain.StudentRepository;
import com.galactique.gestionecole.support.LicensedEstablishmentFixture;
import com.galactique.gestionecole.tuition.application.CreateTuitionFeeCommand;
import com.galactique.gestionecole.tuition.application.TuitionApplicationService;
import com.galactique.gestionecole.tuition.domain.TuitionFeeDraft;
import com.galactique.gestionecole.tuition.domain.TuitionRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class EnrollmentPaymentControllerTests {
	private static final EmployeeActor ACTOR = new EmployeeActor("admin-tech");
	@Autowired MockMvc mvc;
	@Autowired JdbcTemplate jdbc;
	@Autowired EstablishmentRepository schools;
	@Autowired LicenseApplicationService licenses;
	@Autowired AcademicYearRepository years;
	@Autowired SchoolCatalogApplicationService catalog;
	@Autowired StudentRepository students;
	@Autowired TuitionApplicationService tuition;
	@Autowired TuitionRepository fees;

	@Test
	void requiresFullPaymentByDefaultAndRollsBackInsufficientPaymentBeforeAllowingACompleteSettlement() throws Exception {
		var fixture = fixture();
		UUID annualFee = fee(fixture, "Annual", "100", "USD", null);
		UUID classFee = fee(fixture, "Class", "50", "USD", fixture.classId());
		UUID euroFee = fee(fixture, "EUR fee", "30", "EUR", fixture.classId());
		mvc.perform(post(fixture.url()).with(admin("can_enrollment_create"))
			.contentType(MediaType.APPLICATION_JSON).content(request(fixture, false).replace(",\"acceptAdvance\":false", "")))
			.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ENROLLMENT_PAYMENT_REQUIRED"));
		assertNothingSaved(fixture);
		mvc.perform(post(fixture.url()).with(admin("can_enrollment_create", "can_payment_record"))
			.contentType(MediaType.APPLICATION_JSON).content(request(fixture, false, payment(annualFee, "25", "CASH"))))
			.andExpect(status().isConflict());
		assertNothingSaved(fixture);
		String response = mvc.perform(post(fixture.url()).with(admin("can_enrollment_create", "can_payment_record"))
			.contentType(MediaType.APPLICATION_JSON).content(request(fixture, false, payment(annualFee, "100", "CASH"),
				payment(classFee, "50", "CASH"), payment(euroFee, "30", "CASH"))))
			.andExpect(status().isCreated()).andExpect(jsonPath("$.advanceAccepted").value(false))
			.andExpect(jsonPath("$.paymentDecisionBy").value("admin-tech"))
			.andExpect(jsonPath("$.payments", hasSize(3))).andExpect(jsonPath("$.payments[0].balance").value(0))
			.andExpect(jsonPath("$.payments[2].currency").value("EUR"))
			.andReturn().getResponse().getContentAsString();
		String paymentId = JsonPath.read(response, "$.payments[0].id");
		mvc.perform(get(fixture.base() + "/school-years/" + fixture.yearId() + "/payments/" + paymentId + "/receipt").with(admin("can_payment_receipt_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$.receiptNumber").isNotEmpty())
			.andExpect(jsonPath("$.balanceAfterPayment").value(0));
		mvc.perform(post(fixture.url()).with(admin("can_enrollment_create", "can_payment_record"))
			.contentType(MediaType.APPLICATION_JSON).content(request(fixture, false, payment(annualFee, "100", "CASH"))))
			.andExpect(status().isConflict());
		assertEquals(3, count("tuition_payments", fixture.studentId()));
	}

	@Test
	void acceptsAnExplicitAdvanceRetainsItsAuthorAndAllowsLaterPaymentOfTheBalance() throws Exception {
		var fixture = fixture();
		UUID fee = fee(fixture, "Tuition", "100", "USD", fixture.classId());
		fee(fixture, "Annual", "50", "USD", null);
		mvc.perform(post(fixture.url()).with(admin("can_enrollment_create"))
			.contentType(MediaType.APPLICATION_JSON).content(request(fixture, true)))
			.andExpect(status().isConflict());
		assertNothingSaved(fixture);
		String response = mvc.perform(post(fixture.url()).with(admin("can_enrollment_create", "can_payment_record"))
			.contentType(MediaType.APPLICATION_JSON).content(request(fixture, true, payment(fee, "25", "CASH"))))
			.andExpect(status().isCreated()).andExpect(jsonPath("$.advanceAccepted").value(true))
			.andExpect(jsonPath("$.payments[0].amount").value(25)).andExpect(jsonPath("$.payments[0].balance").value(75))
			.andReturn().getResponse().getContentAsString();
		String enrollmentId = JsonPath.read(response, "$.id");
		String chargeId = JsonPath.read(response, "$.payments[0].chargeId");
		mvc.perform(get(fixture.url()).with(admin("can_enrollment_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$[0].advanceAccepted").value(true))
			.andExpect(jsonPath("$[0].paymentDecisionBy").value("admin-tech"));
		mvc.perform(post(fixture.base() + "/school-years/" + fixture.yearId() + "/students/" + fixture.studentId() + "/tuition-fees/" + chargeId + "/payments")
			.with(admin("can_payment_record")).contentType(MediaType.APPLICATION_JSON)
			.content("{\"amount\":75,\"paymentMethodCode\":\"CASH\"}"))
			.andExpect(status().isCreated());
		var room = catalog.createClassroom(fixture.schoolId(), fixture.yearId(), fixture.classId(), "B", "Room B", 40, ACTOR);
		mvc.perform(patch(fixture.url() + "/" + enrollmentId).with(admin("can_enrollment_update"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"classroomId\":\"" + room.id() + "\"}"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.advanceAccepted").value(true))
			.andExpect(jsonPath("$.paymentDecisionBy").value("admin-tech"));
		assertEquals(2, count("student_fee_charges", fixture.studentId()));
		assertEquals(2, count("tuition_payments", fixture.studentId()));
	}

	@Test
	void allowsEnrollmentWithoutPaymentOnlyWhenNoEnabledFeeApplies() throws Exception {
		var fixture = fixture();
		var otherClass = catalog.createClass(fixture.schoolId(), fixture.yearId(),
			new CreateSchoolClassCommand("OTHER", "Other", null), ACTOR);
		fee(fixture, "Other class fee", "100", "USD", otherClass.id());
		UUID disabled = fee(fixture, "Disabled", "100", "USD", null);
		jdbc.update("UPDATE tuition_fee_definitions SET enabled = FALSE WHERE id = ?", disabled);
		mvc.perform(post(fixture.url()).with(admin("can_enrollment_create"))
			.contentType(MediaType.APPLICATION_JSON).content(request(fixture, false)))
			.andExpect(status().isCreated()).andExpect(jsonPath("$.payments", hasSize(0)));
		assertEquals(0, count("student_fee_charges", fixture.studentId()));
	}

	@Test
	void rejectsInvalidPaymentsAndRollsBackAllWritesEvenAfterTheFirstValidPayment() throws Exception {
		var fixture = fixture();
		UUID first = fee(fixture, "First", "100", "USD", null);
		UUID second = fee(fixture, "Second", "50", "USD", fixture.classId());
		for (String[] inputs : List.of(new String[] {payment(first, "10", "CASH"), payment(second, "10", "UNKNOWN")},
			new String[] {payment(first, "10", "CASH"), payment(first, "10", "CASH")},
			new String[] {payment(first, "0", "CASH")}, new String[] {payment(first, "0.001", "CASH")},
			new String[] {payment(first, "10", " ")})) {
			mvc.perform(post(fixture.url()).with(admin("can_enrollment_create", "can_payment_record"))
				.contentType(MediaType.APPLICATION_JSON).content(request(fixture, true, inputs)))
				.andExpect(status().isBadRequest());
			assertNothingSaved(fixture);
		}
		mvc.perform(post(fixture.url()).with(admin("can_enrollment_create", "can_payment_record"))
			.contentType(MediaType.APPLICATION_JSON).content(request(fixture, true, payment(first, "101", "CASH"))))
			.andExpect(status().isConflict());
		assertNothingSaved(fixture);
		mvc.perform(post(fixture.url()).with(admin("can_enrollment_create"))
			.contentType(MediaType.APPLICATION_JSON).content(request(fixture, true, payment(first, "10", "CASH"))))
			.andExpect(status().isForbidden());
		assertNothingSaved(fixture);
	}

	@Test
	void refusesFeesFromAnotherSchoolClassYearOrStudentAndRequiresTheTuitionEntitlement() throws Exception {
		var fixture = fixture();
		UUID validFee = fee(fixture, "Valid", "100", "USD", null);
		var foreign = fixture();
		UUID foreignFee = fee(foreign, "Foreign", "100", "USD", null);
		var otherClass = catalog.createClass(fixture.schoolId(), fixture.yearId(),
			new CreateSchoolClassCommand("OTHER", "Other", null), ACTOR);
		UUID otherClassFee = fee(fixture, "Other", "100", "USD", otherClass.id());
		var oldYear = years.save(fixture.schoolId(), "2025-2026", LocalDate.of(2025, 9, 1), LocalDate.of(2026, 6, 30));
		var type = fees.findFeeTypes(fixture.schoolId()).getFirst();
		UUID oldFee = fees.saveFee(new TuitionFeeDraft(fixture.schoolId(), oldYear.id(), type.id(), type.code(), type.name(),
			"Old fee", new BigDecimal("100"), "USD", LocalDate.of(2026, 1, 1), List.of(), null)).id();
		UUID otherStudent = students.save(fixture.schoolId(), "Other", "Student").id();
		UUID individualFee = fees.saveFee(new TuitionFeeDraft(fixture.schoolId(), fixture.yearId(), type.id(), type.code(), type.name(),
			"Individual fee", new BigDecimal("100"), "USD", LocalDate.of(2026, 10, 1), List.of(), otherStudent)).id();
		for (UUID invalid : List.of(foreignFee, otherClassFee, oldFee, individualFee)) {
			mvc.perform(post(fixture.url()).with(admin("can_enrollment_create", "can_payment_record"))
				.contentType(MediaType.APPLICATION_JSON).content(request(fixture, true, payment(invalid, "10", "CASH"))))
				.andExpect(status().isNotFound());
			assertNothingSaved(fixture);
		}
		var offer = licenses.createOffer("NO-TUITION-" + UUID.randomUUID().toString().substring(0, 8), "School only",
			Set.of(LicenseFeature.SCHOOL_MANAGEMENT, LicenseFeature.STUDENT_RECORDS));
		jdbc.update("UPDATE platform_licenses SET offer_id = ? WHERE establishment_id = ?", offer.id(), fixture.schoolId());
		mvc.perform(post(fixture.url()).with(admin("can_enrollment_create", "can_payment_record"))
			.contentType(MediaType.APPLICATION_JSON).content(request(fixture, false, payment(validFee, "100", "CASH"))))
			.andExpect(status().isForbidden());
		assertEquals(0, count("enrollments", fixture.studentId()));
		assertEquals(0, count("student_fee_charges", fixture.studentId()));
		assertEquals(0, count("tuition_payments", fixture.studentId()));
	}

	@Test
	void recognizesAnAlreadyPaidApplicableChargeWithoutChargingItAgain() throws Exception {
		var fixture = fixture();
		UUID fee = fee(fixture, "Prepaid", "100", "USD", null);
		var definition = fees.findFee(fee, fixture.schoolId(), fixture.yearId()).orElseThrow();
		var student = students.findByIdAndEstablishmentId(fixture.studentId(), fixture.schoolId()).orElseThrow();
		var charge = fees.saveCharge(definition, student);
		tuition.recordPayment(fixture.schoolId(), fixture.yearId(), student.id(), charge.id(), new BigDecimal("100"), "CASH", null, ACTOR);
		mvc.perform(post(fixture.url()).with(admin("can_enrollment_create"))
			.contentType(MediaType.APPLICATION_JSON).content(request(fixture, false)))
			.andExpect(status().isCreated()).andExpect(jsonPath("$.payments", hasSize(0)));
		assertEquals(1, count("student_fee_charges", fixture.studentId()));
		assertEquals(1, count("tuition_payments", fixture.studentId()));
	}

	private Fixture fixture() {
		var school = LicensedEstablishmentFixture.create(schools, licenses,
			"EPM-" + UUID.randomUUID().toString().substring(0, 8), "Enrollment Payment School");
		var year = years.save(school.id(), "2026-2027", LocalDate.of(2026, 9, 1), LocalDate.of(2027, 6, 30));
		var schoolClass = catalog.createClass(school.id(), year.id(),
			new CreateSchoolClassCommand("6", "Sixième", null), ACTOR);
		catalog.createClassroom(school.id(), year.id(), schoolClass.id(), "A", "Sixième A", 40, ACTOR);
		var student = students.save(school.id(), "New", "Student");
		tuition.replacePaymentMethods(school.id(), List.of(new TuitionApplicationService.PaymentMethodInput("CASH", "Cash desk")), ACTOR);
		tuition.createFeeType(school.id(), "TUITION", "Tuition", ACTOR);
		return new Fixture(school.id(), year.id(), schoolClass.id(), student.id());
	}
	private UUID fee(Fixture fixture, String name, String amount, String currency, UUID classId) {
		var type = fees.findFeeTypes(fixture.schoolId()).getFirst();
		return tuition.createFee(fixture.schoolId(), fixture.yearId(), new CreateTuitionFeeCommand(name,
			new BigDecimal(amount), currency, LocalDate.of(2026, 10, 1), type.id(), classId, null, null), ACTOR).fee().id();
	}
	private String request(Fixture fixture, boolean acceptAdvance, String... payments) {
		return "{\"studentId\":\"" + fixture.studentId() + "\",\"schoolClassId\":\"" + fixture.classId()
			+ "\",\"acceptAdvance\":" + acceptAdvance + ",\"payments\":[" + String.join(",", payments) + "]}";
	}
	private String payment(UUID fee, String amount, String method) {
		return "{\"feeId\":\"" + fee + "\",\"amount\":" + amount + ",\"paymentMethodCode\":\"" + method + "\"}";
	}
	private void assertNothingSaved(Fixture fixture) {
		for (String table : List.of("enrollments", "student_fee_charges", "tuition_payments")) {
			assertEquals(0, count(table, fixture.studentId()), table);
		}
	}
	private int count(String table, UUID studentId) {
		return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE student_id = ?", Integer.class, studentId);
	}
	private static RequestPostProcessor admin(String... permissions) {
		var authorities = new java.util.ArrayList<SimpleGrantedAuthority>();
		authorities.add(new SimpleGrantedAuthority("ADMIN_ACCESS"));
		Arrays.stream(permissions).map(SimpleGrantedAuthority::new).forEach(authorities::add);
		return user("admin-tech").authorities(authorities);
	}
	private record Fixture(UUID schoolId, UUID yearId, UUID classId, UUID studentId) {
		String base() { return "/api/v1/establishments/" + schoolId; }
		String url() { return base() + "/school-years/" + yearId + "/enrollments"; }
	}
}
