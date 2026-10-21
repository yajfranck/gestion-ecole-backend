package com.galactique.gestionecole.reporting.api;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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

import com.galactique.gestionecole.attendance.domain.AttendanceRepository;
import com.galactique.gestionecole.attendance.domain.AttendanceStatus;
import com.galactique.gestionecole.employees.domain.EmployeeRepository;
import com.galactique.gestionecole.enrollments.domain.EnrollmentRepository;
import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;
import com.galactique.gestionecole.licenses.domain.PlatformLicense;
import com.galactique.gestionecole.positions.domain.PositionRepository;
import com.galactique.gestionecole.schoolclasses.domain.AcademicYear;
import com.galactique.gestionecole.schoolclasses.domain.AcademicYearRepository;
import com.galactique.gestionecole.schoolclasses.domain.ClassroomRepository;
import com.galactique.gestionecole.schoolclasses.domain.SchoolClassRepository;
import com.galactique.gestionecole.schoolclasses.domain.SubjectRepository;
import com.galactique.gestionecole.security.domain.SecurityAccountProvisioner;
import com.galactique.gestionecole.students.domain.StudentRepository;

@SpringBootTest(properties = "management.health.mail.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class DashboardControllerTests {
	private static final UUID OFFER = UUID.fromString("c51d6db7-309f-4e85-b884-000000000001");
	@Autowired MockMvc mvc;
	@Autowired JdbcTemplate jdbc;
	@Autowired EstablishmentRepository schools;
	@Autowired EmployeeRepository employees;
	@Autowired PositionRepository positions;
	@Autowired SecurityAccountProvisioner accounts;
	@Autowired AcademicYearRepository years;
	@Autowired SchoolClassRepository classes;
	@Autowired ClassroomRepository classrooms;
	@Autowired StudentRepository students;
	@Autowired EnrollmentRepository enrollments;
	@Autowired SubjectRepository subjects;
	@Autowired AttendanceRepository attendance;
	@Autowired LicenseApplicationService licenses;

	@Test
	void platformAdministratorGetsPlatformCountsWithoutSchoolBusinessData() throws Exception {
		String before = mvc.perform(get("/api/v1/dashboard").with(reader("admin-tech")))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		Fixture active = fixture(true);
		Fixture suspended = fixture(true);
		licenses.suspend(suspended.school().id(), suspended.license().id(), "TEST");
		Fixture disabled = fixture(false);
		schools.disable(disabled.school().id());
		Fixture upcoming = fixture(false);
		licenses.assign(upcoming.school().id(), OFFER, Instant.now().plusSeconds(86400), null, "TEST");
		String after = mvc.perform(get("/api/v1/dashboard").with(reader("admin-tech")))
			.andExpect(status().isOk()).andExpect(jsonPath("$.scope").value("PLATFORM"))
			.andExpect(jsonPath("$.establishment").isEmpty())
			.andReturn().getResponse().getContentAsString();
		for (String field : List.of("totalEstablishments", "activeEstablishments", "disabledEstablishments",
			"activeLicenses", "suspendedLicenses", "upcomingLicenses", "establishmentsWithoutActiveLicense")) {
			int increment = switch (field) {
				case "totalEstablishments" -> 4;
				case "activeEstablishments" -> 3;
				case "establishmentsWithoutActiveLicense" -> 2;
				default -> 1;
			};
			assertEquals(number(before, "$.platform." + field) + increment, number(after, "$.platform." + field), field);
		}
		mvc.perform(get("/api/v1/dashboard").param("academicYearId", UUID.randomUUID().toString())
			.with(reader("admin-tech"))).andExpect(status().isBadRequest());
	}

	@Test
	void establishmentDashboardAggregatesOnlyItsOwnYearAndKeepsCurrenciesSeparate() throws Exception {
		Fixture fixture = fixture(true);
		LocalDate today = LocalDate.now(ZoneOffset.UTC);
		AcademicYear year = year(fixture, "Current", today.minusMonths(6), today.plusMonths(6));
		AcademicYear oldYear = year(fixture, "Old", today.minusYears(2), today.minusYears(1));
		var classA = classes.save(fixture.school().id(), year.id(), "A", "Class A", "Scientifique");
		var classB = classes.save(fixture.school().id(), year.id(), "B", "Class B", "Littéraire");
		classes.save(fixture.school().id(), year.id(), "EMPTY", "Empty class", null);
		var roomA = classrooms.save(fixture.school().id(), year.id(), classA.id(), "A", "Room A", 40);
		var roomB = classrooms.save(fixture.school().id(), year.id(), classB.id(), "B", "Room B", 40);
		var first = students.save(fixture.school().id(), "First", "Student");
		var second = students.save(fixture.school().id(), "Second", "Student");
		var third = students.save(fixture.school().id(), "Third", "Student");
		var firstEnrollment = enrollments.save(fixture.school().id(), first.id(), year.id(), classA.id(), roomA.id(), false, fixture.email());
		var secondEnrollment = enrollments.save(fixture.school().id(), second.id(), year.id(), classA.id(), roomA.id(), false, fixture.email());
		enrollments.save(fixture.school().id(), third.id(), year.id(), classB.id(), roomB.id(), false, fixture.email());
		attendance.save(firstEnrollment.id(), today, AttendanceStatus.PRESENT, false, "Internal", "Family", fixture.email());
		attendance.save(secondEnrollment.id(), today, AttendanceStatus.ABSENT, true, null, null, fixture.email());
		var archived = students.save(fixture.school().id(), "Archived", "Student");
		var archivedEnrollment = enrollments.save(fixture.school().id(), archived.id(), year.id(), classA.id(), roomA.id(), false, fixture.email());
		attendance.save(archivedEnrollment.id(), today, AttendanceStatus.PRESENT, false, null, null, fixture.email());
		students.archive(archived.id(), fixture.school().id());
		seedCharge(fixture, year, first.id(), "USD", 100, today.minusDays(1), 20, 30);
		seedCharge(fixture, year, second.id(), "USD", 200, today.plusDays(1), 10);
		seedCharge(fixture, year, third.id(), "EUR", 90, today.minusDays(1), 30);
		seedCharge(fixture, oldYear, first.id(), "USD", 400, today.minusYears(1));
		Fixture foreign = fixture(true);
		var foreignYear = year(foreign, "Other", today.minusMonths(6), today.plusMonths(6));
		var foreignStudent = students.save(foreign.school().id(), "Other", "Student");
		seedCharge(foreign, foreignYear, foreignStudent.id(), "USD", 999, today.minusDays(1));
		UUID subject = subjects.save(fixture.school().id(), "MATH", "Mathematics").id();
		seedContent(fixture, year, classA.id(), subject, "LESSON", "DRAFT");
		seedContent(fixture, year, classA.id(), subject, "LESSON", "ARCHIVED");
		seedContent(fixture, year, classB.id(), subject, "ASSIGNMENT", "PUBLISHED");
		String token = login(fixture);
		mvc.perform(get(dashboardUrl(fixture, year.id()))
			.param("date", today.toString()).param("establishmentId", foreign.school().id().toString())
			.header("Authorization", "Bearer " + token))
			.andExpect(status().isOk()).andExpect(jsonPath("$.scope").value("ESTABLISHMENT"))
			.andExpect(jsonPath("$.platform").isEmpty())
			.andExpect(jsonPath("$.establishment.id").value(fixture.school().id().toString()))
			.andExpect(jsonPath("$.establishment.counts.students").value(3))
			.andExpect(jsonPath("$.establishment.counts.employees").value(1))
			.andExpect(jsonPath("$.establishment.counts.subjects").value(1))
			.andExpect(jsonPath("$.establishment.counts.classes").value(3))
			.andExpect(jsonPath("$.establishment.counts.enrollments").value(3))
			.andExpect(jsonPath("$.establishment.classes[0].students").value(2))
			.andExpect(jsonPath("$.establishment.classes[0].series").value("Scientifique"))
			.andExpect(jsonPath("$.establishment.classes[1].series").value("Littéraire"))
			.andExpect(jsonPath("$.establishment.classes[2].students").value(0))
			.andExpect(jsonPath("$.establishment.attendance.present").value(1))
			.andExpect(jsonPath("$.establishment.attendance.absent").value(1))
			.andExpect(jsonPath("$.establishment.attendance.justifiedAbsent").value(1))
			.andExpect(jsonPath("$.establishment.attendance.notRecorded").value(1))
			.andExpect(jsonPath("$.establishment.finance", hasSize(2)))
			.andExpect(jsonPath("$.establishment.finance[0].currency").value("EUR"))
			.andExpect(jsonPath("$.establishment.finance[0].balance").value(60))
			.andExpect(jsonPath("$.establishment.finance[1].amountBilled").value(300))
			.andExpect(jsonPath("$.establishment.finance[1].amountPaid").value(60))
			.andExpect(jsonPath("$.establishment.finance[1].balance").value(240))
			.andExpect(jsonPath("$.establishment.finance[1].overdueBalance").value(50))
			.andExpect(jsonPath("$.establishment.learning.draftLessons").value(1))
			.andExpect(jsonPath("$.establishment.learning.publishedAssignments").value(1));
		mvc.perform(get(dashboardUrl(fixture, year.id())).with(reader(fixture.email())))
			.andExpect(status().isOk()).andExpect(jsonPath("$.establishment.academicYear.id").value(year.id().toString()));
		mvc.perform(get(dashboardUrl(fixture, foreignYear.id()))
			.with(reader(fixture.email()))).andExpect(status().isNotFound());
		mvc.perform(get(dashboardUrl(fixture, year.id()))
			.param("date", year.endsOn().plusDays(1).toString()).with(reader(fixture.email())))
			.andExpect(status().isBadRequest());
	}

	@Test
	void emptyDashboardAndLicenseTransitionsHaveExplicitStates() throws Exception {
		Fixture fixture = fixture(true);
		var selectedYear = year(fixture, "Selected", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
		mvc.perform(get(dashboardUrl(fixture, years.findAllByEstablishmentId(fixture.school().id()).getLast().id())).with(reader(fixture.email())))
			.andExpect(status().isOk()).andExpect(jsonPath("$.establishment.academicYear.id").value(selectedYear.id().toString()))
			.andExpect(jsonPath("$.establishment.classes", hasSize(0)))
			.andExpect(jsonPath("$.establishment.finance", hasSize(0)))
			.andExpect(jsonPath("$.establishment.attendance.present").value(0));
		licenses.assign(fixture.school().id(), OFFER, Instant.now().plusSeconds(86400), null, "TEST");
		mvc.perform(get(dashboardUrl(fixture, years.findAllByEstablishmentId(fixture.school().id()).getLast().id())).with(reader(fixture.email())))
			.andExpect(status().isOk()).andExpect(jsonPath("$.establishment.license.state").value("ACTIVE"));
		licenses.suspend(fixture.school().id(), fixture.license().id(), "TEST");
		mvc.perform(get(dashboardUrl(fixture, years.findAllByEstablishmentId(fixture.school().id()).getLast().id())).with(reader(fixture.email())))
			.andExpect(status().isOk()).andExpect(jsonPath("$.establishment.license.state").value("SUSPENDED"))
			.andExpect(jsonPath("$.establishment.finance").isEmpty())
			.andExpect(jsonPath("$.establishment.attendance").isEmpty())
			.andExpect(jsonPath("$.establishment.learning").isEmpty());
		Fixture unlicensed = fixture(false);
		var unlicensedYear = year(unlicensed, "Selected", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
		mvc.perform(get(dashboardUrl(unlicensed, unlicensedYear.id())).with(reader(unlicensed.email())))
			.andExpect(status().isOk()).andExpect(jsonPath("$.establishment.license.state").value("UNLICENSED"));
	}

	@Test
	void requiresPermissionAndCurrentAdministratorIdentity() throws Exception {
		mvc.perform(get("/api/v1/dashboard")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/dashboard").with(user("parent@example.org").roles("PARENT")))
			.andExpect(status().isForbidden());
		mvc.perform(get("/api/v1/dashboard").with(reader("unknown@example.org"))).andExpect(status().isForbidden());
		String staff = "staff-" + UUID.randomUUID() + "@example.org";
		accounts.createEmployeeAccount(staff, "dashboard-password-21", Set.of("STAFF"));
		mvc.perform(get("/api/v1/dashboard").with(reader(staff))).andExpect(status().isForbidden());
		Fixture fixture = fixture(true);
		String token = login(fixture);
		accounts.updateEmployeeRoles(fixture.accountId(), Set.of("STAFF"));
		mvc.perform(get("/api/v1/dashboard").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
		Fixture disabled = fixture(true);
		accounts.disableEmployeeAccount(disabled.accountId());
		mvc.perform(get("/api/v1/dashboard").with(reader(disabled.email()))).andExpect(status().isForbidden());
		Fixture disabledSchool = fixture(true);
		schools.disable(disabledSchool.school().id());
		mvc.perform(get(dashboardUrl(disabledSchool, UUID.randomUUID())).with(reader(disabledSchool.email()))).andExpect(status().isNotFound());
		mvc.perform(get("/api/v1/dashboard").param("academicYearId", "invalid").with(reader("admin-tech")))
			.andExpect(status().isBadRequest());
	}

	private String dashboardUrl(Fixture fixture, UUID yearId) {
		return "/api/v1/establishments/" + fixture.school().id() + "/school-years/" + yearId + "/dashboard";
	}

	private Fixture fixture(boolean licensed) {
		Establishment school = schools.save("DSH-" + UUID.randomUUID().toString().substring(0, 8), "Dashboard School");
		PlatformLicense license = licensed ? licenses.assign(school.id(), OFFER, null, null, "TEST") : null;
		var position = positions.save(school.id(), "Administrator", Set.of("ESTABLISHMENT_ADMIN"));
		String email = "dashboard-" + UUID.randomUUID() + "@example.org";
		var employee = employees.save("DSH-" + UUID.randomUUID(), "Dashboard", "Administrator", email, position.id(), Set.of());
		UUID accountId = accounts.createEmployeeAccount(email, "dashboard-password-21", Set.of("ESTABLISHMENT_ADMIN"));
		employees.attachUserAccount(employee.id(), accountId);
		return new Fixture(school, email, accountId, license);
	}
	private AcademicYear year(Fixture fixture, String name, LocalDate start, LocalDate end) {
		return years.save(fixture.school().id(), name, start, end);
	}
	private String login(Fixture fixture) throws Exception {
		String response = mvc.perform(post("/api/v1/login").contentType(MediaType.APPLICATION_JSON)
			.content("{\"login\":\"" + fixture.email() + "\",\"password\":\"dashboard-password-21\"}"))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.accessToken");
	}
	private void seedCharge(Fixture fixture, AcademicYear year, UUID studentId, String currency,
		int amount, LocalDate due, int... payments) {
		UUID feeId = UUID.randomUUID();
		UUID chargeId = UUID.randomUUID();
		jdbc.update("""
			INSERT INTO tuition_fee_definitions (id, establishment_id, academic_year_id, name, amount, currency, due_date, created_at)
			VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
			""", feeId, fixture.school().id(), year.id(), feeId.toString(), amount, currency, due);
		jdbc.update("""
			INSERT INTO student_fee_charges (id, fee_id, establishment_id, academic_year_id, student_id,
			 student_number, student_first_name, student_last_name, name, amount, currency, due_date, created_at)
			VALUES (?, ?, ?, ?, ?, ?, 'First', 'Student', 'Tuition', ?, ?, ?, CURRENT_TIMESTAMP)
			""", chargeId, feeId, fixture.school().id(), year.id(), studentId, studentId.toString(), amount, currency, due);
		for (int payment : payments) {
			jdbc.update("""
				INSERT INTO tuition_payments (id, charge_id, establishment_id, student_id, receipt_number,
				 amount, currency, payment_method_code, payment_method_name, received_at, recorded_by)
				VALUES (?, ?, ?, ?, ?, ?, ?, 'CASH', 'Cash', CURRENT_TIMESTAMP, 'TEST')
				""", UUID.randomUUID(), chargeId, fixture.school().id(), studentId,
				UUID.randomUUID().toString().replace("-", ""), payment, currency);
		}
	}
	private void seedContent(Fixture fixture, AcademicYear year, UUID classId, UUID subject, String type, String state) {
		jdbc.update("""
			INSERT INTO learning_contents (id, establishment_id, academic_year_id, school_class_id, subject_id,
			 type, title, status, created_by, updated_by, created_at, updated_at)
			VALUES (?, ?, ?, ?, ?, ?, 'Content', ?, 'TEST', 'TEST', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
			""", UUID.randomUUID(), fixture.school().id(), year.id(), classId, subject, type, state);
	}
	private static RequestPostProcessor reader(String email) {
		return user(email).authorities(new SimpleGrantedAuthority("can_dashboard_read"));
	}
	private static long number(String json, String path) { return ((Number) JsonPath.read(json, path)).longValue(); }
	private record Fixture(Establishment school, String email, UUID accountId, PlatformLicense license) { }
}
