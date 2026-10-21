package com.galactique.gestionecole.schoolclasses.api;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
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
import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.employees.domain.EmployeeRepository;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;
import com.galactique.gestionecole.positions.domain.PositionRepository;
import com.galactique.gestionecole.schoolclasses.application.CreateClassroomCommand;
import com.galactique.gestionecole.schoolclasses.application.CreateSchoolClassCommand;
import com.galactique.gestionecole.schoolclasses.application.SchoolCatalogApplicationService;
import com.galactique.gestionecole.schoolclasses.domain.AcademicYearRepository;
import com.galactique.gestionecole.security.domain.SecurityAccountProvisioner;
import com.galactique.gestionecole.students.domain.StudentRepository;
import com.galactique.gestionecole.support.LicensedEstablishmentFixture;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ClassroomControllerTests {
	@Autowired MockMvc mvc;
	@Autowired EstablishmentRepository schools;
	@Autowired LicenseApplicationService licenses;
	@Autowired AcademicYearRepository years;
	@Autowired SchoolCatalogApplicationService catalog;
	@Autowired StudentRepository students;
	@Autowired AttendanceRepository attendance;
	@Autowired EmployeeRepository employees;
	@Autowired PositionRepository positions;
	@Autowired SecurityAccountProvisioner accounts;
	@Autowired JdbcTemplate jdbc;

	@Test
	void managesMultipleClassroomsAndValidatesCodesAndArchiving() throws Exception {
		Fixture fixture = fixture();
		String firstRoomId = firstClassroom(fixture);
		mvc.perform(patch(fixture.rooms() + "/" + firstRoomId).with(admin("can_school_class_update"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"code\":\" a \",\"name\":\"Sixième A\"}"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.code").value("A"));
		String secondId = createRoom(fixture, "b", "Sixième B");
		mvc.perform(get(fixture.rooms()).with(admin("can_school_class_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
		mvc.perform(get(fixture.rooms() + "/" + secondId).with(admin("can_school_class_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$.schoolClassId").value(fixture.classId().toString()))
			.andExpect(jsonPath("$.code").value("B"));
		mvc.perform(post(fixture.rooms()).with(admin("can_school_class_create"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"code\":\" b \",\"name\":\"Duplicate\",\"maxStudents\":40}"))
			.andExpect(status().isConflict());
		mvc.perform(post(fixture.rooms()).with(admin("can_school_class_create"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"C\",\"name\":\"   \"}"))
			.andExpect(status().isBadRequest());
		mvc.perform(post(fixture.rooms()).with(admin("can_school_class_create"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"C\",\"name\":\"Room C\",\"maxStudents\":0}"))
			.andExpect(status().isBadRequest());
		mvc.perform(patch(fixture.rooms() + "/" + secondId).with(admin("can_school_class_update"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
			.andExpect(status().isBadRequest());
		mvc.perform(patch(fixture.rooms() + "/" + secondId).with(admin("can_school_class_update"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"A\"}"))
			.andExpect(status().isConflict());
		mvc.perform(delete(fixture.rooms() + "/" + secondId).with(admin("can_school_class_delete")))
			.andExpect(status().isNoContent());
		mvc.perform(get(fixture.rooms()).with(admin("can_school_class_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
		mvc.perform(get(fixture.rooms() + "/" + secondId).with(admin("can_school_class_read")))
			.andExpect(status().isNotFound());
		String studentId = students.save(fixture.schoolId(), "Archived", "Room").id().toString();
		mvc.perform(post(fixture.enrollments()).with(admin("can_enrollment_create"))
			.contentType(MediaType.APPLICATION_JSON).content(enrollment(fixture, studentId, secondId)))
			.andExpect(status().isNotFound());
		mvc.perform(delete(fixture.classUrl()).with(admin("can_school_class_delete"))).andExpect(status().isNoContent());
		mvc.perform(get(fixture.rooms()).with(admin("can_school_class_read"))).andExpect(status().isNotFound());
	}

	@Test
	void assignsExactlyOneRoomAndMovesExistingStudentWithoutRecreatingEnrollmentOrAttendance() throws Exception {
		Fixture fixture = fixture();
		String firstRoom = firstClassroom(fixture);
		String secondRoom = createRoom(fixture, "B", "Sixième B", 3);
		String firstStudent = students.save(fixture.schoolId(), "Ada", "First").id().toString();
		String secondStudent = students.save(fixture.schoolId(), "Grace", "Second").id().toString();
		mvc.perform(post(fixture.enrollments()).with(admin("can_enrollment_create"))
			.contentType(MediaType.APPLICATION_JSON).content(enrollment(fixture, firstStudent, null)))
			.andExpect(status().isBadRequest());
		String response = mvc.perform(post(fixture.enrollments()).with(admin("can_enrollment_create"))
			.contentType(MediaType.APPLICATION_JSON).content(enrollment(fixture, firstStudent, firstRoom)))
			.andExpect(status().isCreated()).andExpect(jsonPath("$.classroomId").value(firstRoom))
			.andReturn().getResponse().getContentAsString();
		String enrollmentId = JsonPath.read(response, "$.id");
		String createdAt = JsonPath.read(response, "$.createdAt");
		attendance.save(UUID.fromString(enrollmentId), LocalDate.of(2026, 10, 1), AttendanceStatus.PRESENT,
			false, null, null, "admin-tech");
		mvc.perform(post(fixture.enrollments()).with(admin("can_enrollment_create"))
			.contentType(MediaType.APPLICATION_JSON).content(enrollment(fixture, firstStudent, secondRoom)))
			.andExpect(status().isConflict());
		mvc.perform(post(fixture.enrollments()).with(admin("can_enrollment_create"))
			.contentType(MediaType.APPLICATION_JSON).content(enrollment(fixture, secondStudent, secondRoom)))
			.andExpect(status().isCreated());
		mvc.perform(patch(fixture.enrollments() + "/" + enrollmentId).with(admin("can_enrollment_update"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"classroomId\":\"" + secondRoom + "\"}"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(enrollmentId))
			.andExpect(jsonPath("$.classroomId").value(secondRoom)).andExpect(jsonPath("$.createdAt").value(createdAt))
			.andExpect(jsonPath("$.schoolClassId").value(fixture.classId().toString()));
		mvc.perform(patch(fixture.rooms() + "/" + secondRoom).with(admin("can_school_class_update"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"maxStudents\":1}"))
			.andExpect(status().isConflict());
		String thirdStudent = students.save(fixture.schoolId(), "Katherine", "Third").id().toString();
		mvc.perform(post(fixture.enrollments()).with(admin("can_enrollment_create"))
			.contentType(MediaType.APPLICATION_JSON).content(enrollment(fixture, thirdStudent, secondRoom)))
			.andExpect(status().isCreated());
		String fourthStudent = students.save(fixture.schoolId(), "Mary", "Fourth").id().toString();
		mvc.perform(post(fixture.enrollments()).with(admin("can_enrollment_create"))
			.contentType(MediaType.APPLICATION_JSON).content(enrollment(fixture, fourthStudent, secondRoom)))
			.andExpect(status().isConflict());
		mvc.perform(get(fixture.classUrl() + "/students").param("classroomId", firstRoom)
			.with(admin("can_enrollment_read"))).andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(0)));
		mvc.perform(delete(fixture.rooms() + "/" + firstRoom).with(admin("can_school_class_delete")))
			.andExpect(status().isNoContent());
		mvc.perform(get(fixture.classUrl() + "/students").param("classroomId", secondRoom)
			.with(admin("can_enrollment_read"))).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)));
		mvc.perform(get(fixture.enrollments()).with(admin("can_enrollment_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)));
		assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM attendance_records WHERE enrollment_id = ?",
			Integer.class, UUID.fromString(enrollmentId)));
		mvc.perform(patch(fixture.enrollments() + "/" + enrollmentId).with(admin("can_enrollment_update"))
			.contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void enforcesPermissionsAndClassYearAndEstablishmentBoundaries() throws Exception {
		Fixture fixture = fixture();
		Fixture foreign = fixture();
		String room = firstClassroom(fixture);
		String foreignRoom = firstClassroom(foreign);
		var otherClass = catalog.createClass(fixture.schoolId(), fixture.yearId(),
			new CreateSchoolClassCommand("OTHER", "Other class", null), new EmployeeActor("admin-tech"));
		var otherClassroom = catalog.createClassroom(fixture.schoolId(), fixture.yearId(), otherClass.id(),
			"A", "Other class A", 40, new EmployeeActor("admin-tech"));
		String otherRoom = otherClassroom.id().toString();
		var otherYear = years.save(fixture.schoolId(), "2027-2028", LocalDate.of(2027, 9, 1), LocalDate.of(2028, 6, 30));
		var nextClass = catalog.createClass(fixture.schoolId(), otherYear.id(),
			new CreateSchoolClassCommand("6", "Sixième", null), new EmployeeActor("admin-tech"));
		var nextClassroom = catalog.createClassroom(fixture.schoolId(), otherYear.id(), nextClass.id(),
			"A", "Sixième A", 40, new EmployeeActor("admin-tech"));
		String nextRoom = nextClassroom.id().toString();
		String student = students.save(fixture.schoolId(), "Scoped", "Student").id().toString();
		for (String invalidRoom : new String[] {foreignRoom, otherRoom, nextRoom}) {
			mvc.perform(post(fixture.enrollments()).with(admin("can_enrollment_create"))
				.contentType(MediaType.APPLICATION_JSON).content(enrollment(fixture, student, invalidRoom)))
				.andExpect(status().isNotFound());
			mvc.perform(get(fixture.classUrl() + "/students").with(admin("can_enrollment_read"))
				.param("classroomId", invalidRoom)).andExpect(status().isNotFound());
		}
		String response = mvc.perform(post(fixture.enrollments()).with(admin("can_enrollment_create"))
			.contentType(MediaType.APPLICATION_JSON).content(enrollment(fixture, student, room)))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		String enrollmentId = JsonPath.read(response, "$.id");
		mvc.perform(patch(fixture.enrollments() + "/" + enrollmentId).with(admin("can_enrollment_create"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"classroomId\":\"" + room + "\"}"))
			.andExpect(status().isForbidden());
		mvc.perform(patch(fixture.enrollments() + "/" + enrollmentId).with(admin("can_enrollment_update"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"classroomId\":\"" + otherRoom + "\"}"))
			.andExpect(status().isNotFound());
		mvc.perform(get(fixture.rooms())).andExpect(status().isUnauthorized());
		mvc.perform(post(fixture.rooms()).with(admin("can_school_class_read"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"B\",\"name\":\"Sixième B\",\"maxStudents\":40}"))
			.andExpect(status().isForbidden());
		var position = positions.save(fixture.schoolId(), "Administrator", Set.of("ESTABLISHMENT_ADMIN"));
		String email = "room-" + UUID.randomUUID() + "@example.org";
		var employee = employees.save("RM-" + UUID.randomUUID(), "Room", "Admin", email, position.id(), Set.of());
		employees.attachUserAccount(employee.id(), accounts.createEmployeeAccount(email, "classroom-password-21",
			Set.of("ESTABLISHMENT_ADMIN")));
		var local = user(email).authorities(new SimpleGrantedAuthority("ESTABLISHMENT_STAFF_MANAGEMENT"),
			new SimpleGrantedAuthority("can_school_class_read"), new SimpleGrantedAuthority("can_enrollment_update"));
		mvc.perform(get(foreign.rooms()).with(local)).andExpect(status().isNotFound());
		mvc.perform(get(fixture.rooms() + "/" + foreignRoom).with(local)).andExpect(status().isNotFound());
		mvc.perform(patch(foreign.enrollments() + "/" + enrollmentId).with(local)
			.contentType(MediaType.APPLICATION_JSON).content("{\"classroomId\":\"" + foreignRoom + "\"}"))
			.andExpect(status().isNotFound());
	}

	private Fixture fixture() {
		var school = LicensedEstablishmentFixture.create(schools, licenses,
			"ROOM-" + UUID.randomUUID().toString().substring(0, 8), "Classroom School");
		var year = years.save(school.id(), "2026-2027", LocalDate.of(2026, 9, 1), LocalDate.of(2027, 6, 30));
		var schoolClass = catalog.createClass(school.id(), year.id(),
			new CreateSchoolClassCommand("6", "Sixième", null), new EmployeeActor("admin-tech"));
		catalog.createClassroom(school.id(), year.id(), schoolClass.id(), "A", "Sixième A", 40,
			new EmployeeActor("admin-tech"));
		return new Fixture(school.id(), year.id(), schoolClass.id());
	}
	private String firstClassroom(Fixture fixture) throws Exception {
		String response = mvc.perform(get(fixture.rooms()).with(admin("can_school_class_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].maxStudents").value(40))
			.andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$[0].id");
	}
	private String createRoom(Fixture fixture, String code, String name) throws Exception {
		return createRoom(fixture, code, name, 40);
	}
	private String createRoom(Fixture fixture, String code, String name, int maxStudents) throws Exception {
		String response = mvc.perform(post(fixture.rooms()).with(admin("can_school_class_create"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"" + code + "\",\"name\":\"" + name
				+ "\",\"maxStudents\":" + maxStudents + "}"))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.id");
	}
	private String enrollment(Fixture fixture, String student, String room) {
		return "{\"studentId\":\"" + student + "\",\"schoolClassId\":\"" + fixture.classId() + "\""
			+ (room == null ? "" : ",\"classroomId\":\"" + room + "\"") + "}";
	}
	private static RequestPostProcessor admin(String permission) {
		return user("admin-tech").authorities(new SimpleGrantedAuthority("ADMIN_ACCESS"), new SimpleGrantedAuthority(permission));
	}
	private record Fixture(UUID schoolId, UUID yearId, UUID classId) {
		String yearUrl() { return "/api/v1/establishments/" + schoolId + "/school-years/" + yearId; }
		String classUrl() { return yearUrl() + "/classes/" + classId; }
		String rooms() { return classUrl() + "/classrooms"; }
		String enrollments() { return yearUrl() + "/enrollments"; }
	}
}
