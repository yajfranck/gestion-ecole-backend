package com.galactique.gestionecole.schoolclasses.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.Set;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;
import com.galactique.gestionecole.schoolclasses.application.CreateSchoolClassCommand;
import com.galactique.gestionecole.schoolclasses.application.SchoolCatalogApplicationService;
import com.galactique.gestionecole.support.LicensedEstablishmentFixture;
import com.galactique.gestionecole.schoolclasses.domain.AcademicYearRepository;
import com.galactique.gestionecole.positions.domain.PositionRepository;
import com.galactique.gestionecole.employees.domain.EmployeeRepository;
import com.galactique.gestionecole.security.domain.SecurityAccountProvisioner;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class SchoolCatalogControllerTests {
	private final MockMvc mockMvc;
	private final EstablishmentRepository establishmentRepository;
	private final LicenseApplicationService licenseApplicationService;
	@Autowired private AcademicYearRepository years;
	@Autowired private PositionRepository positions;
	@Autowired private EmployeeRepository employees;
	@Autowired private SecurityAccountProvisioner accounts;
	@Autowired private SchoolCatalogApplicationService catalog;

	@Autowired
	SchoolCatalogControllerTests(MockMvc mockMvc, EstablishmentRepository establishmentRepository,
		LicenseApplicationService licenseApplicationService) {
		this.mockMvc = mockMvc;
		this.establishmentRepository = establishmentRepository;
		this.licenseApplicationService = licenseApplicationService;
	}

	@Test
	void createsAClassWithoutAnyClassroomAtTheApplicationBoundary() {
		Establishment school = createEstablishment();
		var year = years.save(school.id(), "2026-2027", LocalDate.of(2026, 9, 1), LocalDate.of(2027, 6, 30));
		var schoolClass = catalog.createClass(school.id(), year.id(),
			new CreateSchoolClassCommand("EMPTY", "No classroom", null), new EmployeeActor("admin-tech"));
		assertEquals("EMPTY", schoolClass.code());
		assertTrue(catalog.findClassrooms(school.id(), year.id(), schoolClass.id(), new EmployeeActor("admin-tech")).isEmpty());
	}

	@Test
	void configuresClassesSubjectsAndClassSubjectAssignments() throws Exception {
		Establishment establishment = createEstablishment();
		String establishmentUrl = "/api/v1/establishments/" + establishment.id();
		String subjectResponse = mockMvc.perform(post(establishmentUrl + "/subjects")
			.with(administrator("can_subject_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"subjects\":[{\"code\":\"MATH\",\"name\":\"Mathematics\"}]}"))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String subjectId = JsonPath.read(subjectResponse, "$[0].id");
		mockMvc.perform(get(establishmentUrl + "/subjects/" + subjectId)
			.with(administrator("can_subject_read")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("MATH"));

		String yearResponse = mockMvc.perform(post(establishmentUrl + "/school-years")
			.with(administrator("can_academic_year_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"2026-2027\",\"startsOn\":\"2026-09-01\",\"endsOn\":\"2027-06-30\"}"))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String yearId = JsonPath.read(yearResponse, "$.id");
		String classesUrl = establishmentUrl + "/school-years/" + yearId + "/classes";

		String schoolClassResponse = mockMvc.perform(post(classesUrl)
			.with(administrator("can_school_class_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"P1-A\",\"name\":\"Primary 1 A\"}"))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		String schoolClassId = JsonPath.read(schoolClassResponse, "$.id");
		mockMvc.perform(get(classesUrl + "/" + schoolClassId + "/classrooms")
			.with(administrator("can_school_class_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
		mockMvc.perform(put(classesUrl + "/" + schoolClassId + "/subjects/" + subjectId)
			.with(administrator("can_class_subject_assign"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"classroomTeachers\":[{\"classroomId\":\"" + UUID.randomUUID()
				+ "\",\"employeeId\":\"" + UUID.randomUUID() + "\"}]}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Cette opération est impossible : aucune salle de classe n'est associée à cette classe. Veuillez d'abord lui affecter au moins une salle."));
		mockMvc.perform(post(classesUrl + "/" + schoolClassId + "/classrooms")
			.with(administrator("can_school_class_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"A\",\"name\":\"Primary 1 A\",\"maxStudents\":40}"))
			.andExpect(status().isCreated());
		mockMvc.perform(get(classesUrl + "/" + schoolClassId + "/classrooms")
			.with(administrator("can_school_class_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].code").value("A"))
			.andExpect(jsonPath("$[0].maxStudents").value(40));
		mockMvc.perform(get(classesUrl + "/" + schoolClassId)
			.with(administrator("can_school_class_read")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("P1-A"))
			.andExpect(jsonPath("$.gradeLevelId").doesNotExist());

		mockMvc.perform(put(classesUrl + "/" + schoolClassId + "/subjects/" + subjectId)
			.with(administrator("can_class_subject_assign")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.enabled").value(true));

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
			.delete(establishmentUrl + "/subjects/" + subjectId)
			.with(administrator("can_subject_delete")))
			.andExpect(status().isConflict());

		mockMvc.perform(get(classesUrl + "/" + schoolClassId + "/subjects")
			.with(administrator("can_class_subject_read")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].subjectId").value(subjectId));
	}

	private Establishment createEstablishment() {
		String code = "CAT-" + UUID.randomUUID().toString().substring(0, 8);
		return LicensedEstablishmentFixture.create(establishmentRepository, licenseApplicationService,
			code, "Catalog Test School");
	}

	@Test
	void definesChangesAndRemovesOptionalSeriesWithoutChangingOtherClassFields() throws Exception {
		String classesUrl = classesUrl(createEstablishment());
		String first = createClass(classesUrl, "A", null);
		createClass(classesUrl, "B", "Scientifique");
		mockMvc.perform(get(first).with(administrator("can_school_class_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$.series").value(nullValue()));
		mockMvc.perform(patch(first).with(administrator("can_school_class_update"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"series\":\"  Scientifique  \"}"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.series").value("Scientifique"))
			.andExpect(jsonPath("$.code").value("A")).andExpect(jsonPath("$.name").value("Class A"));
		mockMvc.perform(get(classesUrl).with(administrator("can_school_class_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[*].series", everyItem(org.hamcrest.Matchers.is("Scientifique"))));
		mockMvc.perform(patch(first).with(administrator("can_school_class_update"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"series\":\"Littéraire\"}"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.series").value("Littéraire"));
		mockMvc.perform(patch(first).with(administrator("can_school_class_update"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Renamed class\"}"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.series").value("Littéraire"));
		mockMvc.perform(patch(first).with(administrator("can_school_class_update"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"series\":null}"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.series").value("Littéraire"));
		mockMvc.perform(get(first).with(administrator("can_school_class_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$.series").value("Littéraire"));
		mockMvc.perform(patch(first).with(administrator("can_school_class_update"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"series\":\"   \"}"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.series").value(nullValue()));
		mockMvc.perform(get(first).with(administrator("can_school_class_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$.series").value(nullValue()));
		String noSeries = createClass(classesUrl, "C", "  ");
		mockMvc.perform(get(noSeries).with(administrator("can_school_class_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$.series").value(nullValue()));
		String maximumLength = createClass(classesUrl, "D", "S".repeat(100));
		mockMvc.perform(get(maximumLength).with(administrator("can_school_class_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$.series").value("S".repeat(100)));
	}

	@Test
	void rejectsInvalidSeriesAndBlankClassFields() throws Exception {
		String classesUrl = classesUrl(createEstablishment());
		String schoolClass = createClass(classesUrl, "A", "Scientifique");
		mockMvc.perform(post(classesUrl).with(administrator("can_school_class_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"B\",\"name\":\"Class B\",\"series\":\"" + "S".repeat(101) + "\"}"))
			.andExpect(status().isBadRequest());
		for (String invalid : new String[] {"{\"series\":\"" + "S".repeat(101) + "\"}",
			"{\"code\":\"   \"}", "{\"name\":\"\"}"}) {
			mockMvc.perform(patch(schoolClass).with(administrator("can_school_class_update"))
				.contentType(MediaType.APPLICATION_JSON).content(invalid)).andExpect(status().isBadRequest());
		}
		mockMvc.perform(get(classesUrl).with(administrator("can_school_class_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].series").value("Scientifique"));
	}

	@Test
	void createsClassesInBulkAtomicallyAndReportsInvalidClassIndex() throws Exception {
		String classesUrl = classesUrl(createEstablishment());
		String response = mockMvc.perform(post(classesUrl + "/bulk").with(administrator("can_school_class_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"classes\":[{\"code\":\"BULK-A\",\"name\":\"Sixième A\"},"
				+ "{\"code\":\"BULK-B\",\"name\":\"Sixième B\"}]}") )
			.andExpect(status().isCreated()).andExpect(jsonPath("$", hasSize(2)))
			.andReturn().getResponse().getContentAsString();
		String classId = JsonPath.read(response, "$[0].id");
		mockMvc.perform(get(classesUrl + "/" + classId + "/classrooms").with(administrator("can_school_class_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));

		mockMvc.perform(post(classesUrl + "/bulk").with(administrator("can_school_class_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"classes\":[{\"code\":\"ROLLBACK-A\",\"name\":\"Valid\"},"
				+ "{\"code\":\"ROLLBACK-A\",\"name\":\"Duplicate\"}]}") )
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(
				org.hamcrest.Matchers.containsString("classes[1]")));
		mockMvc.perform(post(classesUrl + "/bulk").with(administrator("can_school_class_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"classes\":[{\"code\":\"ROLLBACK-B\",\"name\":\"Valid\"},"
				+ "{\"code\":\"ROLLBACK-C\",\"name\":\"   \"}]}") )
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(
				org.hamcrest.Matchers.containsString("classes[1]")));
		mockMvc.perform(post(classesUrl + "/bulk").with(administrator("can_school_class_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"classes\":[{\"code\":\"BULK-A\",\"name\":\"Already exists\"}]}") )
			.andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(
				org.hamcrest.Matchers.containsString("classes[0]")));
		mockMvc.perform(get(classesUrl).with(administrator("can_school_class_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
	}

	@Test
	void seriesUpdatesRequirePermissionAndRemainWithinTheAdministratorsEstablishment() throws Exception {
		Establishment school = createEstablishment();
		String ownClasses = classesUrl(school);
		String ownClass = createClass(ownClasses, "A", "Scientifique");
		String foreignClass = createClass(classesUrl(createEstablishment()), "A", "Littéraire");
		var position = positions.save(school.id(), "Administrator", Set.of("ESTABLISHMENT_ADMIN"));
		String email = "series-" + UUID.randomUUID() + "@example.org";
		var employee = employees.save("SER-" + UUID.randomUUID(), "Series", "Administrator", email,
			position.id(), Set.of());
		employees.attachUserAccount(employee.id(), accounts.createEmployeeAccount(email,
			"series-password-21", Set.of("ESTABLISHMENT_ADMIN")));
		var localAdministrator = user(email).authorities(new SimpleGrantedAuthority("ESTABLISHMENT_STAFF_MANAGEMENT"),
			new SimpleGrantedAuthority("can_school_class_update"));
		mockMvc.perform(patch(ownClass).with(user(email).authorities(
			new SimpleGrantedAuthority("ESTABLISHMENT_STAFF_MANAGEMENT")))
			.contentType(MediaType.APPLICATION_JSON).content("{\"series\":\"Littéraire\"}"))
			.andExpect(status().isForbidden());
		mockMvc.perform(patch(ownClass).with(localAdministrator)
			.contentType(MediaType.APPLICATION_JSON).content("{\"series\":\"Littéraire\"}"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.series").value("Littéraire"));
		mockMvc.perform(patch(foreignClass).with(localAdministrator)
			.contentType(MediaType.APPLICATION_JSON).content("{\"series\":\"Scientifique\"}"))
			.andExpect(status().isNotFound());
		String foreignId = foreignClass.substring(foreignClass.lastIndexOf('/') + 1);
		mockMvc.perform(patch(ownClasses + "/" + foreignId).with(localAdministrator)
			.contentType(MediaType.APPLICATION_JSON).content("{\"series\":\"Scientifique\"}"))
			.andExpect(status().isNotFound());
		mockMvc.perform(get(foreignClass).with(administrator("can_school_class_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$.series").value("Littéraire"));
	}

	private String classesUrl(Establishment school) {
		var year = years.save(school.id(), "2026-2027", LocalDate.of(2026, 9, 1), LocalDate.of(2027, 6, 30));
		return "/api/v1/establishments/" + school.id() + "/school-years/" + year.id() + "/classes";
	}

	private String createClass(String classesUrl, String code, String series) throws Exception {
		String seriesJson = series == null ? "" : ",\"series\":\"" + series + "\"";
		String response = mockMvc.perform(post(classesUrl).with(administrator("can_school_class_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"" + code + "\",\"name\":\"Class " + code + "\"" + seriesJson + "}"))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return classesUrl + "/" + JsonPath.read(response, "$.id");
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor administrator(String permission) {
		return user("admin-tech").authorities(
			new SimpleGrantedAuthority("ADMIN_ACCESS"), new SimpleGrantedAuthority(permission));
	}
}
