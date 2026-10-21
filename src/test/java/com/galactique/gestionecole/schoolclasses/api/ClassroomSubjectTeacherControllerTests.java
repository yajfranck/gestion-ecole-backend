package com.galactique.gestionecole.schoolclasses.api;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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

import com.galactique.gestionecole.employees.domain.EmployeeRepository;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;
import com.galactique.gestionecole.positions.domain.PositionRepository;
import com.galactique.gestionecole.schoolclasses.domain.AcademicYearRepository;
import com.galactique.gestionecole.schoolclasses.domain.ClassroomRepository;
import com.galactique.gestionecole.schoolclasses.domain.SchoolClassRepository;
import com.galactique.gestionecole.schoolclasses.domain.SubjectRepository;
import com.galactique.gestionecole.security.domain.SecurityAccountProvisioner;
import com.galactique.gestionecole.support.LicensedEstablishmentFixture;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ClassroomSubjectTeacherControllerTests {
	@Autowired private MockMvc mvc;
	@Autowired private EstablishmentRepository schools;
	@Autowired private LicenseApplicationService licenses;
	@Autowired private AcademicYearRepository years;
	@Autowired private SchoolClassRepository classes;
	@Autowired private ClassroomRepository classrooms;
	@Autowired private SubjectRepository subjects;
	@Autowired private PositionRepository positions;
	@Autowired private EmployeeRepository employees;
	@Autowired private SecurityAccountProvisioner accounts;
	@Autowired private JdbcTemplate jdbc;

	@Test
	void assignsOnePersonToSeveralSubjectsAndRoomsAndAllowsDifferentTeachersPerRoom() throws Exception {
		var f = fixture();
		String first = assign(f, f.math(), teachers(f.roomA(), f.teacher(), f.roomB(), f.teacher()));
		String second = assign(f, f.science(), teachers(f.roomA(), f.teacher(), f.roomB(), f.otherTeacher()));
		assertEquals(2, JsonPath.<List<?>>read(first, "$.classroomTeachers").size());
		assertEquals(f.otherTeacher().toString(), JsonPath.read(second, "$.classroomTeachers[1].employeeId"));
		mvc.perform(get(f.url()).with(admin("can_class_subject_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].classroomTeachers", hasSize(2)))
			.andExpect(jsonPath("$[1].classroomTeachers", hasSize(2)));
		assign(f, f.math(), teachers(f.roomA(), f.teacher(), f.roomB(), f.teacher()));
		assertEquals(4, jdbc.queryForObject("SELECT COUNT(*) FROM classroom_subject_teachers WHERE school_class_id = ?",
			Integer.class, f.schoolClass()));
	}

	@Test
	void replacesOrClearsTeachersWhileKeepingTheSubjectAssignmentAndLegacyRequests() throws Exception {
		var f = fixture();
		String initial = assign(f, f.math(), teachers(f.roomA(), f.teacher(), f.roomB(), f.teacher()));
		String updated = assign(f, f.math(), teachers(f.roomB(), f.otherTeacher()));
		assertEquals(JsonPath.<String>read(initial, "$.id"), JsonPath.read(updated, "$.id"));
		assertEquals(1, JsonPath.<List<?>>read(updated, "$.classroomTeachers").size());
		assertEquals(f.otherTeacher().toString(), JsonPath.read(updated, "$.classroomTeachers[0].employeeId"));
		String cleared = assign(f, f.math(), "{\"classroomTeachers\":[]}");
		assertEquals(0, JsonPath.<List<?>>read(cleared, "$.classroomTeachers").size());
		mvc.perform(put(f.url() + "/" + f.science()).with(admin("can_class_subject_assign")))
			.andExpect(status().isOk()).andExpect(jsonPath("$.classroomTeachers", hasSize(0)));
		mvc.perform(put(f.url() + "/" + f.science()).with(admin("can_class_subject_assign")))
			.andExpect(status().isConflict());
		mvc.perform(delete(f.url() + "/" + f.math()).with(admin("can_class_subject_unassign")))
			.andExpect(status().isNoContent());
		mvc.perform(put(f.url() + "/" + f.math()).with(admin("can_class_subject_assign")))
			.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(JsonPath.<String>read(initial, "$.id")));
	}

	@Test
	void rejectsDuplicateRoomsNullEntriesAndIncompleteOrInvalidIdentifiers() throws Exception {
		var f = fixture();
		for (String body : List.of(teachers(f.roomA(), f.teacher(), f.roomA(), f.otherTeacher()),
			"{\"classroomTeachers\":[null]}", "{\"classroomTeachers\":[{}]}",
			"{\"classroomTeachers\":[{\"classroomId\":\"" + f.roomA() + "\"}]}",
			"{\"classroomTeachers\":[{\"employeeId\":\"" + f.teacher() + "\"}]}",
			"{\"classroomTeachers\":[{\"classroomId\":\"invalid\",\"employeeId\":\"" + f.teacher() + "\"}]}")) {
			mvc.perform(put(f.url() + "/" + f.math()).with(admin("can_class_subject_assign"))
				.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
		}
		mvc.perform(get(f.url()).with(admin("can_class_subject_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void rejectsForeignMissingOrInactiveReferencesWithoutPartiallySavingAssignments() throws Exception {
		var f = fixture();
		var foreign = fixture();
		var otherClass = classes.save(f.school(), f.year(), "OTHER", "Other class", null);
		var otherRoom = classrooms.save(f.school(), f.year(), otherClass.id(), "A", "Other room", 40);
		var nextYear = years.save(f.school(), "2027-2028", LocalDate.of(2027, 9, 1), LocalDate.of(2028, 6, 30));
		var nextClass = classes.save(f.school(), nextYear.id(), "NEXT", "Next year class", null);
		var nextRoom = classrooms.save(f.school(), nextYear.id(), nextClass.id(), "A", "Next year room", 40);
		UUID inactiveTeacher = teacher(f.school(), "STAFF");
		employees.disable(inactiveTeacher);
		jdbc.update("UPDATE classrooms SET enabled = FALSE WHERE id = ?", f.roomB());
		for (String body : List.of(teachers(f.roomA(), f.teacher(), foreign.roomA(), f.teacher()),
			teachers(otherRoom.id(), f.teacher()), teachers(nextRoom.id(), f.teacher()),
			teachers(f.roomA(), foreign.teacher()), teachers(f.roomA(), inactiveTeacher),
			teachers(f.roomB(), f.teacher()), teachers(UUID.randomUUID(), f.teacher()),
			teachers(f.roomA(), UUID.randomUUID()))) {
			mvc.perform(put(f.url() + "/" + f.math()).with(admin("can_class_subject_assign"))
				.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNotFound());
		}
		mvc.perform(get(f.url()).with(admin("can_class_subject_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
		assign(f, f.math(), teachers(f.roomA(), f.teacher()));
		mvc.perform(put(f.url() + "/" + f.math()).with(admin("can_class_subject_assign"))
			.contentType(MediaType.APPLICATION_JSON).content(teachers(f.roomA(), f.otherTeacher(), foreign.roomB(), f.teacher())))
			.andExpect(status().isNotFound());
		mvc.perform(get(f.url()).with(admin("can_class_subject_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$[0].classroomTeachers", hasSize(1)))
			.andExpect(jsonPath("$[0].classroomTeachers[0].employeeId").value(f.teacher().toString()));
	}

	@Test
	void requiresPermissionAndKeepsTheLocalAdministratorWithinTheirSchool() throws Exception {
		var f = fixture();
		var foreign = fixture();
		UUID employeeId = teacher(f.school(), "ESTABLISHMENT_ADMIN");
		var employee = employees.findById(employeeId).orElseThrow();
		employees.attachUserAccount(employeeId, accounts.createEmployeeAccount(employee.email(),
			"teacher-assignment-password-21", Set.of("ESTABLISHMENT_ADMIN")));
		String body = teachers(f.roomA(), f.teacher());
		mvc.perform(put(f.url() + "/" + f.math()).contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isUnauthorized());
		mvc.perform(put(f.url() + "/" + f.math()).with(user(employee.email()).authorities(
			new SimpleGrantedAuthority("ESTABLISHMENT_STAFF_MANAGEMENT")))
			.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
		var actor = user(employee.email()).authorities(new SimpleGrantedAuthority("ESTABLISHMENT_STAFF_MANAGEMENT"),
			new SimpleGrantedAuthority("can_class_subject_assign"));
		mvc.perform(put(f.url() + "/" + f.math()).with(actor).contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isOk());
		mvc.perform(put(foreign.url() + "/" + foreign.math()).with(actor).contentType(MediaType.APPLICATION_JSON)
			.content(teachers(foreign.roomA(), foreign.teacher()))).andExpect(status().isNotFound());
		mvc.perform(get(f.url()).with(user(employee.email())))
			.andExpect(status().isForbidden());
	}

	@Test
	void retainsAssignmentsOnSubjectArchivalAndHidesArchivedStaffAndRooms() throws Exception {
		var f = fixture();
		assign(f, f.math(), teachers(f.roomA(), f.teacher(), f.roomB(), f.otherTeacher()));
		mvc.perform(delete(f.url() + "/" + f.math()).with(admin("can_class_subject_unassign")))
			.andExpect(status().isNoContent());
		mvc.perform(get(f.url()).with(admin("can_class_subject_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
		mvc.perform(put(f.url() + "/" + f.math()).with(admin("can_class_subject_assign")))
			.andExpect(status().isOk()).andExpect(jsonPath("$.classroomTeachers", hasSize(2)));
		employees.disable(f.teacher());
		jdbc.update("UPDATE classrooms SET enabled = FALSE WHERE id = ?", f.roomB());
		mvc.perform(get(f.url()).with(admin("can_class_subject_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$[0].classroomTeachers", hasSize(0)));
	}

	@Test
	void databaseRejectsMismatchedScopesAndDuplicateTeachersForTheSameSubjectAndRoom() throws Exception {
		var f = fixture();
		var foreign = fixture();
		assign(f, f.math(), teachers(f.roomA(), f.teacher()));
		String insert = "INSERT INTO classroom_subject_teachers (establishment_id, academic_year_id, school_class_id, subject_id, classroom_id, employee_id) VALUES (?, ?, ?, ?, ?, ?)";
		assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(insert,
			f.school(), f.year(), f.schoolClass(), f.math(), f.roomA(), f.otherTeacher()));
		assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(insert,
			f.school(), f.year(), f.schoolClass(), f.math(), f.roomB(), foreign.teacher()));
		assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(insert,
			f.school(), f.year(), f.schoolClass(), f.math(), foreign.roomB(), f.teacher()));
		assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(insert,
			f.school(), f.year(), f.schoolClass(), f.science(), f.roomB(), f.teacher()));
	}

	private String assign(Fixture f, UUID subjectId, String body) throws Exception {
		return mvc.perform(put(f.url() + "/" + subjectId).with(admin("can_class_subject_assign"))
			.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
	}

	private static String teachers(UUID... identifiers) {
		var entries = new java.util.ArrayList<String>();
		for (int index = 0; index < identifiers.length; index += 2) {
			entries.add("{\"classroomId\":\"" + identifiers[index] + "\",\"employeeId\":\"" + identifiers[index + 1] + "\"}");
		}
		return "{\"classroomTeachers\":[" + String.join(",", entries) + "]}";
	}

	private Fixture fixture() {
		var school = LicensedEstablishmentFixture.create(schools, licenses,
			"TEACH-" + UUID.randomUUID().toString().substring(0, 8), "Subject teacher school");
		var year = years.save(school.id(), "2026-2027", LocalDate.of(2026, 9, 1), LocalDate.of(2027, 6, 30));
		var schoolClass = classes.save(school.id(), year.id(), "C", "Class", null);
		return new Fixture(school.id(), year.id(), schoolClass.id(),
			classrooms.save(school.id(), year.id(), schoolClass.id(), "A", "Room A", 40).id(),
			classrooms.save(school.id(), year.id(), schoolClass.id(), "B", "Room B", 40).id(),
			subjects.save(school.id(), "Mathematics").id(), subjects.save(school.id(), "Science").id(),
			teacher(school.id(), "STAFF"), teacher(school.id(), "STAFF"));
	}

	private UUID teacher(UUID schoolId, String role) {
		var position = positions.save(schoolId, "Personnel " + UUID.randomUUID(), Set.of(role));
		return employees.save("T-" + UUID.randomUUID(), "Subject", "Teacher", UUID.randomUUID() + "@example.org",
			position.id(), Set.of()).id();
	}

	private static RequestPostProcessor admin(String permission) {
		return user("admin-tech").authorities(new SimpleGrantedAuthority("ADMIN_ACCESS"), new SimpleGrantedAuthority(permission));
	}

	private record Fixture(UUID school, UUID year, UUID schoolClass, UUID roomA, UUID roomB,
		UUID math, UUID science, UUID teacher, UUID otherTeacher) {
		String url() {
			return "/api/v1/establishments/" + school + "/school-years/" + year + "/classes/" + schoolClass + "/subjects";
		}
	}
}
