package com.galactique.gestionecole.timetables.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
import com.galactique.gestionecole.positions.domain.PositionRepository;
import com.galactique.gestionecole.schoolclasses.application.CreateSchoolClassCommand;
import com.galactique.gestionecole.schoolclasses.application.SchoolCatalogApplicationService;
import com.galactique.gestionecole.schoolclasses.domain.AcademicYearRepository;
import com.galactique.gestionecole.schoolclasses.domain.ClassSubjectRepository;
import com.galactique.gestionecole.schoolclasses.domain.SubjectRepository;
import com.galactique.gestionecole.security.domain.SecurityAccountProvisioner;
import com.galactique.gestionecole.support.LicensedEstablishmentFixture;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class TimetableControllerTests {
	private static final EmployeeActor GLOBAL = new EmployeeActor("admin-tech", true, false);
	@Autowired MockMvc mvc;
	@Autowired JdbcTemplate jdbc;
	@Autowired EstablishmentRepository schools;
	@Autowired LicenseApplicationService licenses;
	@Autowired PositionRepository positions;
	@Autowired EmployeeRepository employees;
	@Autowired SecurityAccountProvisioner accounts;
	@Autowired AcademicYearRepository years;
	@Autowired SchoolCatalogApplicationService catalog;
	@Autowired SubjectRepository subjects;
	@Autowired ClassSubjectRepository assignments;

	@Test
	void managesRecurringEntriesAndKeepsDifferentGroupsOfTheSameClassIndependent() throws Exception {
		var f = fixture();
		String id = create(f, f.roomA(), payload(f.subject(), f.teacherA(), "MONDAY", "08:00", "09:00", "\"roomCode\":\" lab-a \""));
		mvc.perform(get(f.url(f.roomA()) + "/" + id).with(f.as("can_timetable_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$.roomCode").value("LAB-A"))
			.andExpect(jsonPath("$.createdBy").value(f.email())).andExpect(jsonPath("$.validFrom").value("2026-09-01"));
		mvc.perform(get(f.url(f.roomA())).param("dayOfWeek", "TUESDAY").with(f.as("can_timetable_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
		mvc.perform(put(f.url(f.roomA()) + "/" + id).with(f.as("can_timetable_update"))
			.contentType(MediaType.APPLICATION_JSON).content(payload(f.subject(), null, "MONDAY", "09:00", "10:00", "")))
			.andExpect(status().isOk()).andExpect(jsonPath("$.teacherId").value(nullValue()))
			.andExpect(jsonPath("$.roomCode").value(nullValue()));
		create(f, f.roomA(), payload(f.subject(), f.teacherA(), "MONDAY", "08:00", "09:00", ""));
		create(f, f.roomB(), payload(f.subject(), f.teacherB(), "MONDAY", "08:00", "09:00", ""));
		mvc.perform(get(f.url(f.roomA())).with(f.as("can_timetable_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].startTime").value("08:00:00"));
		mvc.perform(delete(f.url(f.roomA()) + "/" + id).with(f.as("can_timetable_delete"))).andExpect(status().isNoContent());
		mvc.perform(get(f.url(f.roomA()) + "/" + id).with(f.as("can_timetable_read"))).andExpect(status().isNotFound());
		assertEquals(false, jdbc.queryForObject("SELECT enabled FROM timetable_entries WHERE id = ?", Boolean.class, UUID.fromString(id)));
		mvc.perform(delete(f.classUrl() + "/classrooms/" + f.roomA()).with(user("admin-tech").authorities(
			new SimpleGrantedAuthority("ADMIN_ACCESS"), new SimpleGrantedAuthority("can_school_class_delete"))))
			.andExpect(status().isConflict());
	}

	@Test
	void rejectsOverlappingClassroomsTeachersAndPhysicalRoomsButAcceptsAdjacentAndDisjointPeriods() throws Exception {
		var f = fixture();
		create(f, f.roomA(), payload(f.subject(), f.teacherA(), "MONDAY", "08:00", "09:00", "\"roomCode\":\"LAB\""));
		for (String body : List.of(payload(f.subject(), f.teacherB(), "MONDAY", "08:30", "09:30", ""),
			payload(f.subject(), null, "MONDAY", "08:00", "09:00", ""))) {
			mvc.perform(post(f.url(f.roomA())).with(f.as("can_timetable_create")).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.conflictingEntryId").isNotEmpty());
		}
		for (String body : List.of(payload(f.subject(), f.teacherA(), "MONDAY", "08:30", "09:30", ""),
			payload(f.subject(), f.teacherB(), "MONDAY", "08:00", "09:00", "\"roomCode\":\" lab \""))) {
			mvc.perform(post(f.url(f.roomB())).with(f.as("can_timetable_create")).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isConflict());
		}
		create(f, f.roomB(), payload(f.subject(), f.teacherA(), "MONDAY", "09:00", "10:00", ""));
		create(f, f.roomA(), payload(f.subject(), f.teacherA(), "TUESDAY", "08:00", "09:00",
			"\"validFrom\":\"2026-09-01\",\"validUntil\":\"2026-12-31\""));
		create(f, f.roomB(), payload(f.subject(), f.teacherA(), "TUESDAY", "08:00", "09:00",
			"\"validFrom\":\"2027-01-01\",\"validUntil\":\"2027-06-30\""));
		var year = years.save(f.school(), "Overlapping year", LocalDate.of(2026, 9, 1), LocalDate.of(2027, 6, 30));
		var otherClass = catalog.createClass(f.school(), year.id(), new CreateSchoolClassCommand("Y2", "Other year", null), GLOBAL);
		assignments.save(f.school(), otherClass.id(), f.subject());
		var room = catalog.findClassrooms(f.school(), year.id(), otherClass.id(), GLOBAL).getFirst();
		String otherUrl = "/api/v1/establishments/" + f.school() + "/school-years/" + year.id() + "/classes/" + otherClass.id()
			+ "/classrooms/" + room.id() + "/timetable";
		mvc.perform(post(otherUrl).with(f.as("can_timetable_create")).contentType(MediaType.APPLICATION_JSON)
			.content(payload(f.subject(), f.teacherA(), "MONDAY", "08:00", "09:00", ""))).andExpect(status().isConflict());
	}

	@Test
	void copiesWithoutAssignmentsByDefaultAndCanMapTeachersOrReplaceTheDestinationAtomically() throws Exception {
		var f = fixture();
		String first = create(f, f.roomA(), payload(f.subject(), f.teacherA(), "MONDAY", "08:00", "09:00", "\"roomCode\":\"LAB\""));
		create(f, f.roomA(), payload(f.subject(), f.teacherA(), "TUESDAY", "10:00", "11:00", ""));
		String response = mvc.perform(post(f.url(f.roomA()) + "/copy").with(f.as("can_timetable_copy"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"targetClassroomId\":\"" + f.roomB() + "\"}"))
			.andExpect(status().isCreated()).andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].teacherId").value(nullValue())).andExpect(jsonPath("$[0].roomCode").value(nullValue()))
			.andExpect(jsonPath("$[0].copiedFromId").value(first)).andReturn().getResponse().getContentAsString();
		assertNotEquals(first, JsonPath.read(response, "$[0].id"));
		mvc.perform(post(f.url(f.roomA()) + "/copy").with(f.as("can_timetable_copy"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"targetClassroomId\":\"" + f.roomB() + "\"}"))
			.andExpect(status().isConflict());
		mvc.perform(post(f.url(f.roomA()) + "/copy").with(f.as("can_timetable_copy"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"targetClassroomId\":\"" + f.roomB() + "\",\"replaceExisting\":true}"))
			.andExpect(status().isForbidden());
		mvc.perform(post(f.url(f.roomA()) + "/copy").with(f.as("can_timetable_copy", "can_timetable_delete"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"targetClassroomId\":\"" + f.roomB()
				+ "\",\"replaceExisting\":true,\"copyTeachers\":true}"))
			.andExpect(status().isConflict());
		assertEquals(2, activeCount(f.roomB()));
		mvc.perform(post(f.url(f.roomA()) + "/copy").with(f.as("can_timetable_copy", "can_timetable_delete"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"targetClassroomId\":\"" + f.roomB()
				+ "\",\"replaceExisting\":true,\"copyRoomCodes\":true}"))
			.andExpect(status().isConflict());
		assertEquals(2, activeCount(f.roomB()));
		mvc.perform(post(f.url(f.roomA()) + "/copy").with(f.as("can_timetable_copy", "can_timetable_delete"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"targetClassroomId\":\"" + f.roomB()
				+ "\",\"replaceExisting\":true,\"teacherMappings\":{\"" + f.teacherA() + "\":\"" + f.teacherB() + "\"}}"))
			.andExpect(status().isCreated()).andExpect(jsonPath("$[0].teacherId").value(f.teacherB().toString()));
		assertEquals(2, activeCount(f.roomB()));
		assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM timetable_entries WHERE classroom_id=? AND enabled=FALSE", Integer.class, f.roomB()));
		assertEquals(2, activeCount(f.roomA()));
	}

	@Test
	void failedCrossClassCopyLeavesExistingTargetUntouchedAndRejectsEmptyOrIdenticalSources() throws Exception {
		var f = fixture();
		create(f, f.roomA(), payload(f.subject(), null, "MONDAY", "08:00", "09:00", ""));
		var extraSubject = subjects.save(f.school(), "EXTRA", "Extra");
		assignments.save(f.school(), f.schoolClass(), extraSubject.id());
		create(f, f.roomA(), payload(extraSubject.id(), null, "TUESDAY", "08:00", "09:00", ""));
		var targetClass = catalog.createClass(f.school(), f.year(), new CreateSchoolClassCommand("T", "Target", null), GLOBAL);
		assignments.save(f.school(), targetClass.id(), f.subject());
		var targetRoom = catalog.findClassrooms(f.school(), f.year(), targetClass.id(), GLOBAL).getFirst();
		String targetUrl = f.yearUrl() + "/classes/" + targetClass.id() + "/classrooms/" + targetRoom.id() + "/timetable";
		String existing = mvc.perform(post(targetUrl).with(f.as("can_timetable_create"))
			.contentType(MediaType.APPLICATION_JSON).content(payload(f.subject(), null, "FRIDAY", "08:00", "09:00", "")))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		mvc.perform(post(f.url(f.roomA()) + "/copy").with(f.as("can_timetable_copy", "can_timetable_delete"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"targetClassroomId\":\"" + targetRoom.id() + "\",\"replaceExisting\":true}"))
			.andExpect(status().isNotFound());
		mvc.perform(get(targetUrl).with(f.as("can_timetable_read"))).andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id").value((String) JsonPath.read(existing, "$.id")));
		assignments.save(f.school(), targetClass.id(), extraSubject.id());
		mvc.perform(post(f.url(f.roomA()) + "/copy").with(f.as("can_timetable_copy", "can_timetable_delete"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"targetClassroomId\":\"" + targetRoom.id() + "\",\"replaceExisting\":true}"))
			.andExpect(status().isCreated()).andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].schoolClassId").value(targetClass.id().toString()));
		for (UUID source : List.of(f.roomA(), f.roomB())) {
			mvc.perform(post(f.url(source) + "/copy").with(f.as("can_timetable_copy"))
				.contentType(MediaType.APPLICATION_JSON).content("{\"targetClassroomId\":\"" + f.roomA() + "\"}"))
				.andExpect(status().isBadRequest());
		}
	}

	@Test
	void providesDailyAndWeeklyViewsRespectingNonWorkingDaysAndAcademicYearBounds() throws Exception {
		var f = fixture();
		create(f, f.roomA(), payload(f.subject(), null, "MONDAY", "08:00", "09:00", ""));
		create(f, f.roomA(), payload(f.subject(), null, "TUESDAY", "08:00", "09:00", ""));
		String calendar = f.yearUrl() + "/timetable/non-working-days";
		mvc.perform(put(calendar).with(f.as("can_timetable_update")).contentType(MediaType.APPLICATION_JSON)
			.content("{\"days\":[{\"date\":\"2026-09-07\",\"name\":\" Holiday \"}]}"))
			.andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Holiday"));
		mvc.perform(get(f.url(f.roomA()) + "/day").with(f.as("can_timetable_read")).param("date", "2026-09-07"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.nonWorkingDay").value("Holiday"))
			.andExpect(jsonPath("$.entries", hasSize(0)));
		mvc.perform(get(f.url(f.roomA()) + "/week").with(f.as("can_timetable_read")).param("date", "2026-09-09"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.startsOn").value("2026-09-07"))
			.andExpect(jsonPath("$.days", hasSize(7))).andExpect(jsonPath("$.days[0].entries", hasSize(0)))
			.andExpect(jsonPath("$.days[1].entries", hasSize(1)));
		mvc.perform(get(f.url(f.roomA()) + "/week").with(f.as("can_timetable_read")).param("date", "2026-09-01"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.days[0].withinAcademicYear").value(false));
		mvc.perform(put(calendar).with(f.as("can_timetable_update")).contentType(MediaType.APPLICATION_JSON)
			.content("{\"days\":[{\"date\":\"2026-09-07\",\"name\":\"A\"},{\"date\":\"2026-09-07\",\"name\":\"B\"}]}"))
			.andExpect(status().isBadRequest());
		mvc.perform(get(calendar).with(f.as("can_timetable_read"))).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Holiday"));
		mvc.perform(put(calendar).with(f.as("can_timetable_update")).contentType(MediaType.APPLICATION_JSON).content("{\"days\":[]}"))
			.andExpect(status().isOk());
		mvc.perform(get(f.url(f.roomA()) + "/day").with(f.as("can_timetable_read")).param("date", "2026-09-07"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.entries", hasSize(1)));
		mvc.perform(get(f.url(f.roomA()) + "/day").with(f.as("can_timetable_read")).param("date", "2026-08-31"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void validatesTimesDatesAndAssignmentsAndRejectsCrossTenantOrRevokedAccess() throws Exception {
		var f = fixture();
		var foreign = fixture();
		for (String invalid : List.of(payload(f.subject(), null, "MONDAY", "09:00", "09:00", ""),
			payload(f.subject(), null, "MONDAY", "10:00", "09:00", ""),
			payload(f.subject(), null, "MONDAY", "08:00:30", "09:00", ""),
			payload(f.subject(), null, "MONDAY", "08:00", "09:00", "\"validFrom\":\"2026-08-01\""),
			payload(f.subject(), null, "MONDAY", "08:00", "09:00", "\"validFrom\":\"2026-09-01\",\"validUntil\":\"2026-09-02\""))) {
			mvc.perform(post(f.url(f.roomA())).with(f.as("can_timetable_create")).contentType(MediaType.APPLICATION_JSON).content(invalid))
				.andExpect(status().isBadRequest());
		}
		for (String invalid : List.of(payload(foreign.subject(), null, "MONDAY", "08:00", "09:00", ""),
			payload(f.subject(), foreign.teacherA(), "MONDAY", "08:00", "09:00", ""))) {
			mvc.perform(post(f.url(f.roomA())).with(f.as("can_timetable_create")).contentType(MediaType.APPLICATION_JSON).content(invalid))
				.andExpect(status().isNotFound());
		}
		mvc.perform(get(foreign.url(foreign.roomA())).with(f.as("can_timetable_read"))).andExpect(status().isNotFound());
		mvc.perform(post(f.url(f.roomA()) + "/copy").with(f.as("can_timetable_copy"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"targetClassroomId\":\"" + foreign.roomA() + "\"}"))
			.andExpect(status().isNotFound());
		mvc.perform(get(f.url(f.roomA()))).andExpect(status().isUnauthorized());
		mvc.perform(get(f.url(f.roomA())).with(f.as("can_timetable_create"))).andExpect(status().isForbidden());
		mvc.perform(get(f.url(f.roomA())).with(user("admin-tech").authorities(new SimpleGrantedAuthority("can_timetable_read"))))
			.andExpect(status().isForbidden());
		String id = create(f, f.roomA(), payload(f.subject(), f.teacherA(), "MONDAY", "08:00", "09:00", ""));
		assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("UPDATE timetable_entries SET teacher_id=? WHERE id=?",
			foreign.teacherA(), UUID.fromString(id)));
		assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("UPDATE timetable_entries SET classroom_id=? WHERE id=?",
			foreign.roomA(), UUID.fromString(id)));
		String foreignEntry = create(foreign, foreign.roomA(), payload(foreign.subject(), null, "TUESDAY", "08:00", "09:00", ""));
		assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("UPDATE timetable_entries SET copied_from_id=? WHERE id=?",
			UUID.fromString(foreignEntry), UUID.fromString(id)));
		accounts.updateEmployeeRoles(f.account(), Set.of("STAFF"));
		mvc.perform(get(f.url(f.roomA())).with(f.as("can_timetable_read"))).andExpect(status().isForbidden());
		jdbc.update("UPDATE platform_licenses SET suspended_at=CURRENT_TIMESTAMP WHERE establishment_id=?", foreign.school());
		mvc.perform(get(foreign.url(foreign.roomA())).with(foreign.as("can_timetable_read"))).andExpect(status().isForbidden());
	}

	@Test
	void concurrentTeacherBookingsAreSerializedEvenWhenBothClassroomsStartEmpty() throws Exception {
		var f = fixture();
		var start = new CountDownLatch(1);
		try (var pool = Executors.newFixedThreadPool(2)) {
			var first = pool.submit(() -> {
				assertTrue(start.await(5, TimeUnit.SECONDS));
				return mvc.perform(post(f.url(f.roomA())).with(f.as("can_timetable_create")).contentType(MediaType.APPLICATION_JSON)
					.content(payload(f.subject(), f.teacherA(), "MONDAY", "08:00", "09:00", ""))).andReturn().getResponse().getStatus();
			});
			var second = pool.submit(() -> {
				assertTrue(start.await(5, TimeUnit.SECONDS));
				return mvc.perform(post(f.url(f.roomB())).with(f.as("can_timetable_create")).contentType(MediaType.APPLICATION_JSON)
					.content(payload(f.subject(), f.teacherA(), "MONDAY", "08:00", "09:00", ""))).andReturn().getResponse().getStatus();
			});
			start.countDown();
			assertEquals(Set.of(201, 409), Set.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)));
		}
		assertEquals(1, activeCount(f.roomA()) + activeCount(f.roomB()));
	}

	private Fixture fixture() {
		var school = LicensedEstablishmentFixture.create(schools, licenses, "TT-" + UUID.randomUUID().toString().substring(0, 8), "Timetable School");
		var year = years.save(school.id(), "2026-2027", LocalDate.of(2026, 9, 1), LocalDate.of(2027, 6, 30));
		var schoolClass = catalog.createClass(school.id(), year.id(), new CreateSchoolClassCommand("6", "Sixième", null), GLOBAL);
		UUID roomA = catalog.findClassrooms(school.id(), year.id(), schoolClass.id(), GLOBAL).getFirst().id();
		UUID roomB = catalog.createClassroom(school.id(), year.id(), schoolClass.id(), "B", "Sixième B", GLOBAL).id();
		var subject = subjects.save(school.id(), "MATH", "Mathematics");
		assignments.save(school.id(), schoolClass.id(), subject.id());
		var adminPosition = positions.save(school.id(), "Administrator", Set.of("ESTABLISHMENT_ADMIN"));
		String email = "timetable-" + UUID.randomUUID() + "@example.org";
		var admin = employees.save("TT-" + UUID.randomUUID(), "Time", "Admin", email, adminPosition.id(), Set.of());
		UUID account = accounts.createEmployeeAccount(email, "timetable-password-21", Set.of("ESTABLISHMENT_ADMIN"));
		employees.attachUserAccount(admin.id(), account);
		var teacherPosition = positions.save(school.id(), "Teacher", Set.of("STAFF"));
		var teacherA = employees.save("TA-" + UUID.randomUUID(), "Teacher", "A", "ta-" + UUID.randomUUID() + "@example.org", teacherPosition.id(), Set.of());
		var teacherB = employees.save("TB-" + UUID.randomUUID(), "Teacher", "B", "tb-" + UUID.randomUUID() + "@example.org", teacherPosition.id(), Set.of());
		return new Fixture(school.id(), year.id(), schoolClass.id(), roomA, roomB, subject.id(), teacherA.id(), teacherB.id(), email, account);
	}
	private String payload(UUID subject, UUID teacher, String day, String start, String end, String extra) {
		return "{\"subjectId\":\"" + subject + "\",\"teacherId\":" + (teacher == null ? "null" : "\"" + teacher + "\"")
			+ ",\"dayOfWeek\":\"" + day + "\",\"startTime\":\"" + start + "\",\"endTime\":\"" + end + "\""
			+ (extra.isBlank() ? "" : "," + extra) + "}";
	}
	private String create(Fixture f, UUID room, String payload) throws Exception {
		String result = mvc.perform(post(f.url(room)).with(f.as("can_timetable_create")).contentType(MediaType.APPLICATION_JSON).content(payload))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(result, "$.id");
	}
	private int activeCount(UUID room) {
		return jdbc.queryForObject("SELECT COUNT(*) FROM timetable_entries WHERE classroom_id=? AND enabled=TRUE", Integer.class, room);
	}
	private record Fixture(UUID school, UUID year, UUID schoolClass, UUID roomA, UUID roomB, UUID subject,
		UUID teacherA, UUID teacherB, String email, UUID account) {
		String yearUrl() { return "/api/v1/establishments/" + school + "/school-years/" + year; }
		String classUrl() { return yearUrl() + "/classes/" + schoolClass; }
		String url(UUID classroom) { return classUrl() + "/classrooms/" + classroom + "/timetable"; }
		RequestPostProcessor as(String... permissions) {
			return user(email).authorities(Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList());
		}
	}
}
