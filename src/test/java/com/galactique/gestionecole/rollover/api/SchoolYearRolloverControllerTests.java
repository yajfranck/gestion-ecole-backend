package com.galactique.gestionecole.rollover.api;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.employees.domain.EmployeeRepository;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;
import com.galactique.gestionecole.licenses.domain.LicenseFeature;
import com.galactique.gestionecole.positions.domain.PositionRepository;
import com.galactique.gestionecole.schoolclasses.domain.*;
import com.galactique.gestionecole.security.domain.SecurityAccountProvisioner;
import com.galactique.gestionecole.security.infrastructure.jwt.JwtTokenService;
import com.galactique.gestionecole.students.domain.StudentRepository;
import com.galactique.gestionecole.support.LicensedEstablishmentFixture;
import com.galactique.gestionecole.tuition.application.TuitionApplicationService;
import com.galactique.gestionecole.tuition.domain.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class SchoolYearRolloverControllerTests {
	private static final EmployeeActor GLOBAL = new EmployeeActor("admin-tech");
	@Autowired MockMvc mvc;
	@Autowired JdbcTemplate jdbc;
	@Autowired EstablishmentRepository schools;
	@Autowired LicenseApplicationService licenses;
	@Autowired PositionRepository positions;
	@Autowired EmployeeRepository employees;
	@Autowired SecurityAccountProvisioner accounts;
	@Autowired AcademicYearRepository years;
	@Autowired SchoolClassRepository classes;
	@Autowired ClassroomRepository rooms;
	@Autowired SubjectRepository subjects;
	@Autowired ClassSubjectRepository assignments;
	@Autowired TuitionRepository tuition;
	@Autowired TuitionApplicationService finance;
	@Autowired StudentRepository students;

	@Test
	void copiesSelectedArchivedRoomsSeriesSubjectsAndOnlyFinancialConfigurationIdempotently() throws Exception {
		var f = fixture();
		var source = classes.findByIdAndEstablishmentId(f.classId(), f.school()).orElseThrow();
		var subject = subjects.save(f.school(), "MATH", "Maths");
		assignments.save(f.school(), source.id(), subject.id());
		var skipped = subjects.save(f.school(), "OLD", "Old subject");
		assignments.save(f.school(), source.id(), skipped.id());
		jdbc.update("UPDATE subjects SET enabled=FALSE WHERE id=?", skipped.id());
		var roomB = rooms.save(f.school(), f.sourceYear(), source.id(), "B", "Sixième B", 40);
		var unselected = rooms.save(f.school(), f.sourceYear(), source.id(), "C", "Sixième C", 40);
		var classFee = fee(f, "Tuition", List.of(source.id()), null, LocalDate.of(2026, 1, 15));
		var annual = fee(f, "Annual", List.of(), null, LocalDate.of(2025, 10, 1));
		var formerStudent = students.save(f.school(), "Former", "Student");
		var individual = fee(f, "Individual", List.of(), formerStudent.id(), LocalDate.of(2026, 1, 15));
		var charge = tuition.saveCharge(classFee, formerStudent);
		finance.recordPayment(f.school(), f.sourceYear(), formerStudent.id(), charge.id(), new BigDecimal("25"), "CASH", "advance", GLOBAL);
		var existingClass = classes.save(f.school(), f.targetYear(), source.code(), source.name(), source.series());
		var existingRoom = rooms.save(f.school(), f.targetYear(), existingClass.id(), "A", "Sixième A", 40);
		var existingStudent = students.save(f.school(), "Already", "Enrolled");
		enroll(f, existingStudent.id(), existingClass.id(), existingRoom.id(), "").andExpect(status().isCreated());
		var before = ledger(f.school());
		jdbc.update("UPDATE academic_years SET enabled=FALSE WHERE id=?", f.sourceYear());
		jdbc.update("UPDATE school_classes SET enabled=FALSE WHERE id=?", source.id());
		jdbc.update("UPDATE classrooms SET enabled=FALSE WHERE academic_year_id=?", f.sourceYear());
		mvc.perform(get(f.url()).with(f.as())).andExpect(status().isOk())
			.andExpect(jsonPath("$.sourceYear.id").value(f.sourceYear().toString()))
			.andExpect(jsonPath("$.classrooms", hasSize(3))).andExpect(jsonPath("$.fees", hasSize(2)))
			.andExpect(jsonPath("$.classrooms[0].subjects", hasSize(1)))
			.andExpect(jsonPath("$.feeTypes[0].id").value(f.type().id().toString()))
			.andExpect(jsonPath("$.paymentMethods[0].code").value("CASH"));
		String result = copy(f, List.of(f.roomId(), roomB.id()), "");
		assertEquals(existingClass.id().toString(), JsonPath.read(result, "$.classes[0].targetId"));
		assertEquals(false, JsonPath.read(result, "$.classes[0].created"));
		assertEquals(1, (Integer) JsonPath.read(result, "$.subjectAssignmentsCreated"));
		assertEquals(skipped.id().toString(), JsonPath.read(result, "$.skippedSubjectIds[0]"));
		assertEquals(2, count("classrooms", f.targetYear()));
		assertEquals(1, count("school_classes", f.targetYear()));
		assertEquals(2, count("tuition_fee_definitions", f.targetYear()));
		assertEquals(before, ledger(f.school()));
		var copied = tuition.findFees(f.school(), f.targetYear());
		var copiedClassFee = copied.stream().filter(x -> x.name().equals("Tuition")).findFirst().orElseThrow();
		assertEquals(List.of(existingClass.id()), copiedClassFee.schoolClassIds());
		assertEquals(LocalDate.of(2027, 1, 15), copiedClassFee.dueDate());
		assertEquals(classFee.amount(), copiedClassFee.amount());
		assertEquals(classFee.currency(), copiedClassFee.currency());
		assertEquals(classFee.feeTypeId(), copiedClassFee.feeTypeId());
		assertFalse(copied.stream().anyMatch(x -> x.name().equals(individual.name())));
		assertTrue(copied.stream().filter(x -> x.name().equals(annual.name())).findFirst().orElseThrow().schoolClassIds().isEmpty());
		String again = copy(f, List.of(f.roomId(), roomB.id()), "");
		assertEquals(false, JsonPath.read(again, "$.classrooms[0].created"));
		assertEquals(0, (Integer) JsonPath.read(again, "$.subjectAssignmentsCreated"));
		assertEquals(2, count("tuition_fee_definitions", f.targetYear()));
		assertEquals(before, ledger(f.school()));
		assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM school_year_rollover_classrooms WHERE source_id=?", Integer.class, unselected.id()));
		var newStudent = students.save(f.school(), "Next", "Student");
		enroll(f, newStudent.id(), existingClass.id(), existingRoom.id(), "").andExpect(status().isConflict());
		enroll(f, newStudent.id(), existingClass.id(), existingRoom.id(), ",\"acceptAdvance\":true,\"payments\":[{\"feeId\":\""
			+ copiedClassFee.id() + "\",\"amount\":10,\"paymentMethodCode\":\"CASH\"}]").andExpect(status().isCreated());
		assertEquals(2, tuition.findChargesForStudentAndYear(f.school(), newStudent.id(), f.targetYear()).size());
		assertEquals(1, tuition.findChargesForStudentAndYear(f.school(), formerStudent.id(), f.sourceYear()).size());
		assertEquals(0, tuition.findChargesForStudentAndYear(f.school(), existingStudent.id(), f.targetYear()).size());
	}

	@Test
	void progressiveBatchesExtendOneSharedFeeAndDoNotCreateUnselectedRoomsOrDefaultRooms() throws Exception {
		var f = fixture();
		var other = classes.save(f.school(), f.sourceYear(), "5", "Cinquième", null);
		var otherRoom = rooms.save(f.school(), f.sourceYear(), other.id(), "A", "Cinquième A", 40);
		var shared = fee(f, "Shared", List.of(f.classId(), other.id()), null, LocalDate.of(2026, 2, 1));
		fee(f, "Annual", List.of(), null, LocalDate.of(2025, 9, 15));
		copy(f, List.of(f.roomId()), ",\"includeAnnualFees\":false");
		assertEquals(1, count("tuition_fee_definitions", f.targetYear()));
		copy(f, List.of(otherRoom.id()), "");
		var definitions = tuition.findFees(f.school(), f.targetYear());
		var target = definitions.stream().filter(x -> x.name().equals(shared.name())).findFirst().orElseThrow();
		assertEquals(2, target.schoolClassIds().size());
		assertNull(target.schoolClassId());
		assertEquals(2, definitions.size());
		assertEquals(2, count("classrooms", f.targetYear()));
		assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM classrooms WHERE academic_year_id=? AND code='DEFAULT'", Integer.class, f.targetYear()));
		copy(f, List.of(f.roomId(), otherRoom.id()), "");
		assertEquals(2, count("tuition_fee_definitions", f.targetYear()));
		assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM school_year_rollover_fees WHERE target_academic_year_id=?", Integer.class, f.targetYear()));
	}

	@Test
	void configurationConflictsRollBackAllClassesRoomsSubjectsFeesAndLinks() throws Exception {
		var f = fixture();
		var subject = subjects.save(f.school(), "ENG", "English");
		assignments.save(f.school(), f.classId(), subject.id());
		var source = fee(f, "Conflict", List.of(f.classId()), null, LocalDate.of(2026, 1, 1));
		tuition.saveFee(new TuitionFeeDraft(f.school(), f.targetYear(), f.type().id(), f.type().code(), f.type().name(), source.name(),
			new BigDecimal("999"), "USD", LocalDate.of(2027, 1, 1), List.of(), null));
		request(f, List.of(f.roomId()), "").andExpect(status().isConflict());
		assertEquals(0, count("school_classes", f.targetYear()));
		assertEquals(0, count("classrooms", f.targetYear()));
		assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM school_year_rollover_classes WHERE target_academic_year_id=?", Integer.class, f.targetYear()));
		assertEquals(1, count("tuition_fee_definitions", f.targetYear()));
		var conflict = classes.save(f.school(), f.targetYear(), "6", "Different", null);
		request(f, List.of(f.roomId()), "").andExpect(status().isConflict());
		classes.archive(conflict.id(), f.school(), f.targetYear());
		request(f, List.of(f.roomId()), "").andExpect(status().isConflict());
	}

	@Test
	void handlesLeapDatesOverridesArchivedTypesAndReactivatesSelectedSubjectAssignments() throws Exception {
		var f = fixture();
		years.update(f.sourceYear(), f.school(), "2023-2024", LocalDate.of(2023, 9, 1), LocalDate.of(2024, 6, 30));
		years.update(f.targetYear(), f.school(), "2024-2025", LocalDate.of(2024, 9, 1), LocalDate.of(2025, 6, 30));
		fee(f, "Leap", List.of(), null, LocalDate.of(2024, 2, 29));
		copy(f, List.of(f.roomId()), "");
		assertEquals(LocalDate.of(2025, 2, 28), tuition.findFees(f.school(), f.targetYear()).getFirst().dueDate());
		var g = fixture();
		years.update(g.targetYear(), g.school(), "Short year", LocalDate.of(2026, 10, 1), LocalDate.of(2027, 6, 30));
		var invalidDate = fee(g, "Early", List.of(g.classId()), null, LocalDate.of(2025, 9, 15));
		request(g, List.of(g.roomId()), "").andExpect(status().isBadRequest());
		assertEquals(0, count("school_classes", g.targetYear()));
		jdbc.update("UPDATE tuition_fee_types SET enabled=FALSE WHERE id=?", g.type().id());
		request(g, List.of(g.roomId()), override(invalidDate.id(), "2026-10-15", null)).andExpect(status().isBadRequest());
		var replacement = tuition.saveFeeType(new TuitionFeeTypeDraft(g.school(), "NEW", "Replacement"));
		var existing = classes.save(g.school(), g.targetYear(), "6", "Sixième", "Scientifique");
		var subject = subjects.save(g.school(), "SCI", "Science");
		assignments.save(g.school(), g.classId(), subject.id());
		var inactive = assignments.save(g.school(), existing.id(), subject.id());
		assignments.disable(inactive.id(), g.school());
		copy(g, List.of(g.roomId()), override(invalidDate.id(), "2026-10-15", replacement.id()));
		var fee = tuition.findFees(g.school(), g.targetYear()).getFirst();
		assertEquals(replacement.id(), fee.feeTypeId());
		assertEquals(LocalDate.of(2026, 10, 15), fee.dueDate());
		assertTrue(assignments.findByEstablishmentIdAndClassIdAndSubjectId(g.school(), existing.id(), subject.id()).orElseThrow().enabled());
	}

	@Test
	void validatesPreviousYearSelectionTenantIsolationInputsAndDatabaseScopes() throws Exception {
		var f = fixture();
		var foreign = fixture();
		var older = years.save(f.school(), "Older", LocalDate.of(2024, 9, 1), LocalDate.of(2025, 6, 30));
		mvc.perform(get(f.url()).with(f.as())).andExpect(status().isOk()).andExpect(jsonPath("$.sourceYear.id").value(f.sourceYear().toString()));
		mvc.perform(get(f.url().replace(f.targetYear().toString(), older.id().toString())).with(f.as())).andExpect(status().isOk())
			.andExpect(jsonPath("$.sourceYear").value(nullValue())).andExpect(jsonPath("$.classrooms", hasSize(0)));
		mvc.perform(post(f.url().replace(f.targetYear().toString(), older.id().toString())).with(f.as())
			.contentType(MediaType.APPLICATION_JSON).content(body(f, List.of(f.roomId()), ""))).andExpect(status().isNotFound());
		request(f, List.of(f.roomId()), "").andExpect(status().isOk());
		mvc.perform(post(f.url()).with(f.as()).contentType(MediaType.APPLICATION_JSON)
			.content(body(f, List.of(f.roomId()), "").replace(f.sourceYear().toString(), older.id().toString())))
			.andExpect(status().isBadRequest());
		request(f, List.of(foreign.roomId()), "").andExpect(status().isNotFound());
		request(f, List.of(f.roomId(), f.roomId()), "").andExpect(status().isBadRequest());
		request(f, List.of(), "").andExpect(status().isBadRequest());
		request(f, List.of(f.roomId()), override(UUID.randomUUID(), "2026-10-01", null)).andExpect(status().isBadRequest());
		mvc.perform(get(foreign.url()).with(f.as())).andExpect(status().isForbidden());
		String result = copy(f, List.of(f.roomId()), "");
		UUID targetRoom = UUID.fromString(JsonPath.read(result, "$.classrooms[0].targetId"));
		assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("UPDATE school_year_rollover_classrooms SET target_id=? WHERE target_id=?", foreign.roomId(), targetRoom));
		jdbc.update("UPDATE classrooms SET enabled=FALSE WHERE id=?", targetRoom);
		request(f, List.of(f.roomId()), "").andExpect(status().isConflict());
	}

	@Test
	void requiresActiveScopedPermissionAndLicensesAndDoesNotSilentlyOmitFees() throws Exception {
		var f = fixture();
		mvc.perform(get(f.url())).andExpect(status().isUnauthorized());
		mvc.perform(get(f.url()).with(user(f.email()).authorities(new SimpleGrantedAuthority("can_school_class_create"))))
			.andExpect(status().isForbidden());
		mvc.perform(get(f.url()).with(user("admin-tech").authorities(new SimpleGrantedAuthority("can_school_year_rollover"))))
			.andExpect(status().isForbidden());
		var offer = licenses.createOffer("RO-" + UUID.randomUUID().toString().substring(0, 8), "School only", Set.of(LicenseFeature.SCHOOL_MANAGEMENT));
		jdbc.update("UPDATE platform_licenses SET offer_id=? WHERE establishment_id=?", offer.id(), f.school());
		fee(f, "Tuition", List.of(f.classId()), null, LocalDate.of(2026, 1, 1));
		mvc.perform(get(f.url()).with(f.as())).andExpect(status().isOk()).andExpect(jsonPath("$.financeAvailable").value(false))
			.andExpect(jsonPath("$.fees", hasSize(0)));
		request(f, List.of(f.roomId()), "").andExpect(status().isForbidden());
		assertEquals(0, count("school_classes", f.targetYear()));
		accounts.updateEmployeeRoles(f.account(), Set.of("STAFF"));
		mvc.perform(get(f.url()).with(currentToken(f.email()))).andExpect(status().isForbidden());
		var g = fixture();
		jdbc.update("UPDATE platform_licenses SET suspended_at=CURRENT_TIMESTAMP WHERE establishment_id=?", g.school());
		mvc.perform(get(g.url()).with(g.as())).andExpect(status().isForbidden());
	}

	@Autowired JwtTokenService tokens;
	private org.springframework.test.web.servlet.request.RequestPostProcessor currentToken(String username) {
		String token = tokens.issue(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(username, null, List.of())).value();
		return request -> { request.addHeader("Authorization", "Bearer " + token); return request; };
	}

	@Test
	void concurrentRetriesCreateOnlyOneClassroomAndFee() throws Exception {
		var f = fixture();
		fee(f, "Tuition", List.of(f.classId()), null, LocalDate.of(2026, 1, 1));
		var start = new CountDownLatch(1);
		try (var pool = Executors.newFixedThreadPool(2)) {
			Callable<Integer> copy = () -> { assertTrue(start.await(5, TimeUnit.SECONDS)); return request(f, List.of(f.roomId()), "").andReturn().getResponse().getStatus(); };
			var first = pool.submit(copy); var second = pool.submit(copy);
			start.countDown();
			assertEquals(200, first.get(10, TimeUnit.SECONDS));
			assertEquals(200, second.get(10, TimeUnit.SECONDS));
		}
		assertEquals(1, count("school_classes", f.targetYear()));
		assertEquals(1, count("classrooms", f.targetYear()));
		assertEquals(1, count("tuition_fee_definitions", f.targetYear()));
	}

	private Fixture fixture() {
		var school = LicensedEstablishmentFixture.create(schools, licenses, "RO-" + UUID.randomUUID().toString().substring(0, 8), "Rollover school");
		var source = years.save(school.id(), "2025-2026", LocalDate.of(2025, 9, 1), LocalDate.of(2026, 6, 30));
		var target = years.save(school.id(), "2026-2027", LocalDate.of(2026, 9, 1), LocalDate.of(2027, 6, 30));
		var schoolClass = classes.save(school.id(), source.id(), "6", "Sixième", "Scientifique");
		var room = rooms.save(school.id(), source.id(), schoolClass.id(), "A", "Sixième A", 40);
		var position = positions.save(school.id(), "Administrator", Set.of("ESTABLISHMENT_ADMIN"));
		String email = "rollover-" + UUID.randomUUID() + "@example.org";
		var employee = employees.save("RO-" + UUID.randomUUID(), "Rollover", "Admin", email, position.id(), Set.of());
		UUID account = accounts.createEmployeeAccount(email, "rollover-password-21", Set.of("ESTABLISHMENT_ADMIN"));
		employees.attachUserAccount(employee.id(), account);
		var type = tuition.saveFeeType(new TuitionFeeTypeDraft(school.id(), "TUITION", "Tuition"));
		finance.replacePaymentMethods(school.id(), List.of(new TuitionApplicationService.PaymentMethodInput("CASH", "Cash desk")), GLOBAL);
		return new Fixture(school.id(), source.id(), target.id(), schoolClass.id(), room.id(), type, email, account);
	}
	private TuitionFee fee(Fixture f, String name, List<UUID> classIds, UUID student, LocalDate date) {
		return tuition.saveFee(new TuitionFeeDraft(f.school(), f.sourceYear(), f.type().id(), f.type().code(), f.type().name(), name,
			new BigDecimal("100.00"), "USD", date, classIds, student));
	}
	private String body(Fixture f, List<UUID> selected, String extra) {
		return "{\"sourceAcademicYearId\":\"" + f.sourceYear() + "\",\"classroomIds\":["
			+ selected.stream().map(id -> "\"" + id + "\"").collect(java.util.stream.Collectors.joining(",")) + "]" + extra + "}";
	}
	private String override(UUID fee, String date, UUID type) {
		return ",\"feeOverrides\":{\"" + fee + "\":{\"dueDate\":\"" + date + "\"" + (type == null ? "" : ",\"feeTypeId\":\"" + type + "\"") + "}}";
	}
	private org.springframework.test.web.servlet.ResultActions request(Fixture f, List<UUID> selected, String extra) throws Exception {
		return mvc.perform(post(f.url()).with(f.as()).contentType(MediaType.APPLICATION_JSON).content(body(f, selected, extra)));
	}
	private String copy(Fixture f, List<UUID> selected, String extra) throws Exception {
		return request(f, selected, extra).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
	}
	private org.springframework.test.web.servlet.ResultActions enroll(Fixture f, UUID student, UUID schoolClass, UUID room, String extra) throws Exception {
		return mvc.perform(post("/api/v1/establishments/" + f.school() + "/school-years/" + f.targetYear() + "/enrollments")
			.with(user("admin-tech").authorities(new SimpleGrantedAuthority("ADMIN_ACCESS"), new SimpleGrantedAuthority("can_enrollment_create"),
				new SimpleGrantedAuthority("can_payment_record")))
			.contentType(MediaType.APPLICATION_JSON).content("{\"studentId\":\"" + student + "\",\"schoolClassId\":\"" + schoolClass
				+ "\",\"classroomId\":\"" + room + "\"" + extra + "}"));
	}
	private int count(String table, UUID year) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE academic_year_id=?", Integer.class, year); }
	private List<List<Map<String, Object>>> ledger(UUID school) {
		return List.of("enrollments", "student_fee_charges", "tuition_payments").stream()
			.map(table -> jdbc.queryForList("SELECT * FROM " + table + " WHERE establishment_id=? ORDER BY id", school)).toList();
	}
	private record Fixture(UUID school, UUID sourceYear, UUID targetYear, UUID classId, UUID roomId,
		TuitionFeeType type, String email, UUID account) {
		String url() { return "/api/v1/establishments/" + school + "/school-years/" + targetYear + "/classrooms/rollover"; }
		RequestPostProcessor as() { return user(email).authorities(new SimpleGrantedAuthority("can_school_year_rollover")); }
	}
}
