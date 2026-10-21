package com.galactique.gestionecole.learning.api;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.learning.application.LearningContentApplicationService;
import com.galactique.gestionecole.learning.application.LearningContentCommand;
import com.galactique.gestionecole.learning.application.LearningContentValidationException;
import com.galactique.gestionecole.learning.domain.LearningContentType;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;

@SpringBootTest(properties = "management.health.mail.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LearningContentControllerTests {
	private final MockMvc mockMvc;
	private final EstablishmentRepository establishmentRepository;
	private final LicenseApplicationService licenseApplicationService;
	private final LearningContentApplicationService learningContentApplicationService;

	@Autowired
	LearningContentControllerTests(MockMvc mockMvc, EstablishmentRepository establishmentRepository,
		LicenseApplicationService licenseApplicationService,
		LearningContentApplicationService learningContentApplicationService) {
		this.mockMvc = mockMvc;
		this.establishmentRepository = establishmentRepository;
		this.licenseApplicationService = licenseApplicationService;
		this.learningContentApplicationService = learningContentApplicationService;
	}

	@Test
	void contentMustBePublishedBeforeItCanBeReturnedByPublishedRoute() throws Exception {
		Establishment establishment = establishmentRepository.save(
			"LRN-" + UUID.randomUUID().toString().substring(0, 8), "Learning Test School");
		licenseApplicationService.assign(establishment.id(),
			UUID.fromString("c51d6db7-309f-4e85-b884-000000000001"), null, null, "TEST_FIXTURE");
		String base = "/api/v1/establishments/" + establishment.id();
		String yearId = createAcademicYear(base);
		String classId = createClass(base, yearId);
		String learningBase = base + "/school-years/" + yearId + "/classes/" + classId + "/learning-content";
		String subjectId = createSubject(base);
		assignSubject(learningBase, subjectId);
		MvcResult created = mockMvc.perform(post(learningBase)
			.with(admin("can_learning_content_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"type\":\"LESSON\",\"title\":\"Fractions\",\"description\":\"Unit 2\","
				+ "\"body\":\"Equivalent fractions\",\"subjectId\":\"" + subjectId + "\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.schoolClassId").value(classId))
			.andExpect(jsonPath("$.subjectId").value(subjectId)).andReturn();
		String contentId = com.jayway.jsonpath.JsonPath.read(
			created.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get(learningBase).with(admin("can_learning_content_read")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.contents", hasSize(1)))
			.andExpect(jsonPath("$.contents[0].status").value("DRAFT"));

		mockMvc.perform(post(learningBase + "/" + contentId + "/publish")
			.with(admin("can_learning_content_publish")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("PUBLISHED"));
	}

	@Test
	void staffCanCreateClassLevelLearningContentWithoutAClassroom() throws Exception {
		Establishment establishment = establishmentRepository.save(
			"LRN-" + UUID.randomUUID().toString().substring(0, 8), "Learning Test School");
		licenseApplicationService.assign(establishment.id(),
			UUID.fromString("c51d6db7-309f-4e85-b884-000000000001"), null, null, "TEST_FIXTURE");
		String base = "/api/v1/establishments/" + establishment.id();
		String yearId = createAcademicYear(base);
		String classId = createClassWithoutClassroom(base, yearId);
		String learningBase = base + "/school-years/" + yearId + "/classes/" + classId + "/learning-content";
		String subjectId = createSubject(base);
		assignSubject(learningBase, subjectId);
		mockMvc.perform(post(learningBase).with(admin("can_learning_content_create"))
			.contentType(MediaType.APPLICATION_JSON).content(contentRequest(LearningContentType.LESSON, subjectId)))
			.andExpect(status().isCreated()).andExpect(jsonPath("$.schoolClassId").value(classId));
	}

	@ParameterizedTest
	@EnumSource(LearningContentType.class)
	void contentRequiresASubjectAssignedToItsClass(LearningContentType type) throws Exception {
		LearningContext context = createContext();
		String unassignedSubjectId = createSubject(context.base());
		LearningContext foreignContext = createContext();
		for (String subjectField : java.util.List.of("", ",\"subjectId\":null",
			",\"subjectId\":\"" + unassignedSubjectId + "\"",
			",\"subjectId\":\"" + foreignContext.subjectId() + "\"")) {
			mockMvc.perform(post(context.learningBase()).with(admin("can_learning_content_create"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"type\":\"" + type + "\",\"title\":\"Fractions\"" + subjectField + "}"))
				.andExpect(status().isBadRequest());
		}
		assertThrows(LearningContentValidationException.class, () -> learningContentApplicationService.create(
			context.establishmentId(), UUID.fromString(context.yearId()), UUID.fromString(context.classId()),
			new LearningContentCommand(type, "Fractions", null, null, null, null),
			new EmployeeActor("admin-tech")));
		mockMvc.perform(get(context.learningBase()).with(admin("can_learning_content_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$.contents", hasSize(0)));
		mockMvc.perform(post(context.learningBase()).with(admin("can_learning_content_create"))
			.contentType(MediaType.APPLICATION_JSON).content(contentRequest(type, context.subjectId())))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.schoolClassId").value(context.classId()))
			.andExpect(jsonPath("$.subjectId").value(context.subjectId()));
	}

	@ParameterizedTest
	@EnumSource(LearningContentType.class)
	void updatesAndPublicationKeepTheContentAssignedToItsClassAndSubject(LearningContentType type) throws Exception {
		LearningContext context = createContext();
		String learningBase = context.learningBase();
		MvcResult created = mockMvc.perform(post(learningBase).with(admin("can_learning_content_create"))
			.contentType(MediaType.APPLICATION_JSON).content(contentRequest(type, context.subjectId())))
			.andExpect(status().isCreated()).andReturn();
		String contentId = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");
		String contentUrl = learningBase + "/" + contentId;
		String nextSubjectId = createSubject(context.base());
		for (String body : java.util.List.of(
			"{\"type\":\"" + type + "\",\"title\":\"Missing subject\"}",
			"{\"type\":\"" + type + "\",\"title\":\"Null subject\",\"subjectId\":null}",
			contentRequest(type, nextSubjectId))) {
			mockMvc.perform(put(contentUrl).with(admin("can_learning_content_update"))
				.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
		}
		mockMvc.perform(get(contentUrl).with(admin("can_learning_content_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$.subjectId").value(context.subjectId()));
		LearningContext foreignContext = createContext();
		mockMvc.perform(put(foreignContext.learningBase() + "/" + contentId)
			.with(admin("can_learning_content_update"))
			.contentType(MediaType.APPLICATION_JSON).content(contentRequest(type, foreignContext.subjectId())))
			.andExpect(status().isNotFound());
		assignSubject(learningBase, nextSubjectId);
		mockMvc.perform(put(contentUrl).with(admin("can_learning_content_update"))
			.contentType(MediaType.APPLICATION_JSON).content(contentRequest(type, nextSubjectId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.schoolClassId").value(context.classId()))
			.andExpect(jsonPath("$.subjectId").value(nextSubjectId));
		mockMvc.perform(delete(classSubjectsUrl(learningBase) + "/" + nextSubjectId)
			.with(admin("can_class_subject_unassign"))).andExpect(status().isNoContent());
		mockMvc.perform(post(contentUrl + "/publish").with(admin("can_learning_content_publish")))
			.andExpect(status().isBadRequest());
		mockMvc.perform(get(contentUrl).with(admin("can_learning_content_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DRAFT"));
		assignSubject(learningBase, nextSubjectId);
		mockMvc.perform(post(contentUrl + "/publish").with(admin("can_learning_content_publish")))
			.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PUBLISHED"));
	}

	private LearningContext createContext() throws Exception {
		Establishment establishment = establishmentRepository.save(
			"LRN-" + UUID.randomUUID().toString().substring(0, 8), "Learning Test School");
		licenseApplicationService.assign(establishment.id(),
			UUID.fromString("c51d6db7-309f-4e85-b884-000000000001"), null, null, "TEST_FIXTURE");
		String base = "/api/v1/establishments/" + establishment.id();
		String yearId = createAcademicYear(base);
		String classId = createClass(base, yearId);
		String learningBase = base + "/school-years/" + yearId + "/classes/" + classId + "/learning-content";
		String subjectId = createSubject(base);
		assignSubject(learningBase, subjectId);
		return new LearningContext(establishment.id(), base, yearId, classId, learningBase, subjectId);
	}

	private String createSubject(String base) throws Exception {
		MvcResult result = mockMvc.perform(post(base + "/subjects").with(admin("can_subject_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"subjects\":[{\"code\":\"SUB-" + UUID.randomUUID().toString().substring(0, 8)
				+ "\",\"name\":\"Subject " + UUID.randomUUID() + "\"}]}"))
			.andExpect(status().isCreated()).andReturn();
		return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$[0].id");
	}

	private void assignSubject(String learningBase, String subjectId) throws Exception {
		mockMvc.perform(put(classSubjectsUrl(learningBase) + "/" + subjectId)
			.with(admin("can_class_subject_assign"))).andExpect(status().isOk());
	}

	private static String classSubjectsUrl(String learningBase) {
		return learningBase.substring(0, learningBase.lastIndexOf('/')) + "/subjects";
	}

	private static String contentRequest(LearningContentType type, String subjectId) {
		return "{\"type\":\"" + type + "\",\"title\":\"Fractions\",\"subjectId\":\"" + subjectId + "\"}";
	}

	private record LearningContext(UUID establishmentId, String base, String yearId, String classId,
		String learningBase, String subjectId) {
	}

	private String createAcademicYear(String base) throws Exception {
		MvcResult result = mockMvc.perform(post(base + "/school-years")
			.with(admin("can_academic_year_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"2026-2027\",\"startsOn\":\"2026-01-01\",\"endsOn\":\"2026-12-31\"}"))
			.andExpect(status().isCreated()).andReturn();
		return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String createClass(String base, String academicYearId) throws Exception {
		MvcResult result = mockMvc.perform(post(base + "/school-years/" + academicYearId + "/classes")
			.with(admin("can_school_class_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"LEARNING\",\"name\":\"Learning Class\"}"))
			.andExpect(status().isCreated()).andReturn();
		String classId = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
		mockMvc.perform(post(base + "/school-years/" + academicYearId + "/classes/" + classId + "/classrooms")
			.with(admin("can_school_class_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"A\",\"name\":\"Learning Class A\",\"maxStudents\":40}"))
			.andExpect(status().isCreated());
		return classId;
	}

	private String createClassWithoutClassroom(String base, String academicYearId) throws Exception {
		MvcResult result = mockMvc.perform(post(base + "/school-years/" + academicYearId + "/classes")
			.with(admin("can_school_class_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"EMPTY\",\"name\":\"Class without room\"}"))
			.andExpect(status().isCreated()).andReturn();
		return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor admin(String permission) {
		return user("admin-tech").authorities(new SimpleGrantedAuthority("ADMIN_ACCESS"),
			new SimpleGrantedAuthority(permission));
	}
}
