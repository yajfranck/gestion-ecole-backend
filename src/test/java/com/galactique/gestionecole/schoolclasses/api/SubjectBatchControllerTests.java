package com.galactique.gestionecole.schoolclasses.api;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.galactique.gestionecole.employees.domain.EmployeeRepository;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;
import com.galactique.gestionecole.positions.domain.PositionRepository;
import com.galactique.gestionecole.security.domain.SecurityAccountProvisioner;
import com.galactique.gestionecole.support.LicensedEstablishmentFixture;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class SubjectBatchControllerTests {
	@Autowired private MockMvc mvc;
	@Autowired private EstablishmentRepository schools;
	@Autowired private LicenseApplicationService licenses;
	@Autowired private EmployeeRepository employees;
	@Autowired private PositionRepository positions;
	@Autowired private SecurityAccountProvisioner accounts;

	@Test
	void createsSeveralSubjectsByNameInRequestOrderAndSupportsTheExistingLifecycle() throws Exception {
		String url = subjectsUrl(createSchool());
		String response = mvc.perform(post(url).with(admin("can_subject_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"subjects\":[{\"name\":\" Mathématiques \"},{\"name\":\"Français\"}]}"))
			.andExpect(status().isCreated()).andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].name").value("Mathématiques"))
			.andExpect(jsonPath("$[1].name").value("Français"))
			.andExpect(jsonPath("$[0].code").doesNotExist())
			.andExpect(jsonPath("$[1].code").doesNotExist())
			.andReturn().getResponse().getContentAsString();
		String subjectId = JsonPath.read(response, "$[0].id");
		assertNotEquals(subjectId, JsonPath.read(response, "$[1].id"));
		mvc.perform(get(url).with(admin("can_subject_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].name").value("Français"));
		mvc.perform(get(url + "/" + subjectId).with(admin("can_subject_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Mathématiques"))
			.andExpect(jsonPath("$.code").doesNotExist());
		mvc.perform(patch(url + "/" + subjectId).with(admin("can_subject_update"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" Mathématiques avancées \"}"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Mathématiques avancées"))
			.andExpect(jsonPath("$.code").doesNotExist());
		mvc.perform(post(url).with(admin("can_subject_create"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"subjects\":[{\"name\":\" Histoire \"}]}"))
			.andExpect(status().isCreated()).andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].name").value("Histoire"))
			.andExpect(jsonPath("$[0].code").doesNotExist());
		mvc.perform(delete(url + "/" + subjectId).with(admin("can_subject_delete")))
			.andExpect(status().isNoContent());
		mvc.perform(get(url + "/" + subjectId).with(admin("can_subject_read")))
			.andExpect(status().isNotFound());
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "/batch"})
	void rejectsInvalidBatchesWithoutCreatingAnySubjects(String suffix) throws Exception {
		String url = subjectsUrl(createSchool());
		for (String body : List.of("null", "{}", "{\"name\":\"Mathématiques\"}", "{\"subjects\":null}", "{\"subjects\":[]}",
			"{\"subjects\":[{\"name\":\"Valid\"},null]}", "{\"subjects\":[{\"name\":\"Valid\"},{}]}",
			"{\"subjects\":[{\"name\":\"Valid\"},{\"name\":null}]}",
			"{\"subjects\":[{\"name\":\"Valid\"},{\"name\":\"   \"}]}",
			"{\"subjects\":[{\"name\":\"Valid\"},{\"name\":\"" + "X".repeat(101) + "\"}]}",
			"{\"subjects\":[{\"name\":\"Valid\"},{\"name\":\"Science\",\"code\":\"   \"}]}",
			"{\"subjects\":[{\"name\":\"Valid\"},{\"name\":\"Science\",\"code\":\"" + "X".repeat(41) + "\"}]}", batch(101))) {
			mvc.perform(post(url + suffix).with(admin("can_subject_create"))
				.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
		}
		mvc.perform(get(url).with(admin("can_subject_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "/batch"})
	void acceptsOneSubjectOrTheMaximumBatchSize(String suffix) throws Exception {
		String url = subjectsUrl(createSchool());
		mvc.perform(post(url + suffix).with(admin("can_subject_create"))
			.contentType(MediaType.APPLICATION_JSON).content(batch(1)))
			.andExpect(status().isCreated()).andExpect(jsonPath("$", hasSize(1)));
		mvc.perform(post(url + suffix).with(admin("can_subject_create"))
			.contentType(MediaType.APPLICATION_JSON).content(batch(100)))
			.andExpect(status().isCreated()).andExpect(jsonPath("$", hasSize(100)));
		mvc.perform(get(url).with(admin("can_subject_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(101)));
	}

	@Test
	void validatesNameOnlyUpdates() throws Exception {
		String url = subjectsUrl(createSchool());
		String response = mvc.perform(post(url).with(admin("can_subject_create"))
			.contentType(MediaType.APPLICATION_JSON).content("{\"subjects\":[{\"name\":\"Mathématiques\"}]}"))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		String subjectId = JsonPath.read(response, "$[0].id");
		for (String body : List.of("{}", "{\"name\":null}", "{\"name\":\"\"}", "{\"name\":\"  \"}",
			"{\"name\":\"" + "X".repeat(101) + "\"}")) {
			mvc.perform(patch(url + "/" + subjectId).with(admin("can_subject_update"))
				.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
		}
		mvc.perform(get(url + "/" + subjectId).with(admin("can_subject_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Mathématiques"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "/batch"})
	void requiresCreatePermissionAndProtectsTheEstablishmentScope(String suffix) throws Exception {
		UUID schoolId = createSchool();
		String url = subjectsUrl(schoolId);
		String foreignUrl = subjectsUrl(createSchool());
		String email = "subject-batch-" + UUID.randomUUID() + "@example.org";
		var position = positions.save(schoolId, "Administrator", Set.of("ESTABLISHMENT_ADMIN"));
		var employee = employees.save("SUB-" + UUID.randomUUID(), "School", "Administrator", email, position.id(), Set.of());
		employees.attachUserAccount(employee.id(), accounts.createEmployeeAccount(email,
			"subject-batch-password-21", Set.of("ESTABLISHMENT_ADMIN")));
		var localAdmin = user(email).authorities(new SimpleGrantedAuthority("ESTABLISHMENT_STAFF_MANAGEMENT"),
			new SimpleGrantedAuthority("can_subject_create"));
		mvc.perform(post(url + suffix).contentType(MediaType.APPLICATION_JSON).content(batch(2)))
			.andExpect(status().isUnauthorized());
		mvc.perform(post(url + suffix).with(user(email).authorities(
			new SimpleGrantedAuthority("ESTABLISHMENT_STAFF_MANAGEMENT")))
			.contentType(MediaType.APPLICATION_JSON).content(batch(2))).andExpect(status().isForbidden());
		mvc.perform(post(url + suffix).with(localAdmin).contentType(MediaType.APPLICATION_JSON).content(batch(2)))
			.andExpect(status().isCreated());
		mvc.perform(post(foreignUrl + suffix).with(localAdmin).contentType(MediaType.APPLICATION_JSON).content(batch(2)))
			.andExpect(status().isNotFound());
		mvc.perform(post(subjectsUrl(UUID.randomUUID()) + suffix).with(admin("can_subject_create"))
			.contentType(MediaType.APPLICATION_JSON).content(batch(2))).andExpect(status().isForbidden());
		mvc.perform(get(foreignUrl).with(admin("can_subject_read")))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "/batch"})
	void rollsBackTheWholeBatchWhenACodeConflicts(String suffix) throws Exception {
		String url = subjectsUrl(createSchool());
		mvc.perform(post(url + suffix).with(admin("can_subject_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"subjects\":[{\"code\":\" math \",\"name\":\"Mathématiques\"}]}"))
			.andExpect(status().isCreated()).andExpect(jsonPath("$[0].code").value("MATH"));
		for (String body : List.of(
			"{\"subjects\":[{\"code\":\"SCI\",\"name\":\"Sciences\"},{\"code\":\"math\",\"name\":\"Doublon existant\"}]}",
			"{\"subjects\":[{\"code\":\"ENG\",\"name\":\"Anglais\"},{\"code\":\" eng \",\"name\":\"Doublon du lot\"}]}")) {
			mvc.perform(post(url + suffix).with(admin("can_subject_create"))
				.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
			mvc.perform(get(url).with(admin("can_subject_read")))
				.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].code").value("MATH"));
		}
	}

	private UUID createSchool() {
		return LicensedEstablishmentFixture.create(schools, licenses,
			"SUB-" + UUID.randomUUID().toString().substring(0, 8), "Subject batch school").id();
	}

	private static String subjectsUrl(UUID schoolId) {
		return "/api/v1/establishments/" + schoolId + "/subjects";
	}

	private static String batch(int size) {
		return "{\"subjects\":[" + IntStream.range(0, size).mapToObj(index -> "{\"name\":\"Subject " + index + "\"}")
			.collect(Collectors.joining(",")) + "]}";
	}

	private static RequestPostProcessor admin(String permission) {
		return user("admin-tech").authorities(new SimpleGrantedAuthority("ADMIN_ACCESS"), new SimpleGrantedAuthority(permission));
	}
}
