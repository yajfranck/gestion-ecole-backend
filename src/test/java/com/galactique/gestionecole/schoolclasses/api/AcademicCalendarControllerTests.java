package com.galactique.gestionecole.schoolclasses.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;
import com.galactique.gestionecole.support.LicensedEstablishmentFixture;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AcademicCalendarControllerTests {
	private final MockMvc mockMvc;
	private final EstablishmentRepository establishmentRepository;
	private final LicenseApplicationService licenseApplicationService;

	@Autowired
	AcademicCalendarControllerTests(MockMvc mockMvc, EstablishmentRepository establishmentRepository,
		LicenseApplicationService licenseApplicationService) {
		this.mockMvc = mockMvc;
		this.establishmentRepository = establishmentRepository;
		this.licenseApplicationService = licenseApplicationService;
	}

	@Test
	void configuresSchoolYearAndNonOverlappingPeriods() throws Exception {
		Establishment establishment = createEstablishment();
		String yearUrl = yearUrl(establishment.id());
		String yearResponse = mockMvc.perform(post(yearUrl)
			.with(administrator("can_academic_year_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"2026-2027\",\"startsOn\":\"2026-09-01\",\"endsOn\":\"2027-06-30\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("2026-2027"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String schoolYearId = JsonPath.read(yearResponse, "$.id");
		String periodUrl = yearUrl + "/" + schoolYearId + "/periods";

		mockMvc.perform(post(periodUrl)
			.with(administrator("can_academic_period_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"Période 1\",\"startsOn\":\"2026-09-01\",\"endsOn\":\"2026-12-20\"}"))
			.andExpect(status().isCreated());

		mockMvc.perform(post(periodUrl)
			.with(administrator("can_academic_period_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"Période 2\",\"startsOn\":\"2026-12-15\",\"endsOn\":\"2027-03-20\"}"))
			.andExpect(status().isConflict());

		mockMvc.perform(post(periodUrl)
			.with(administrator("can_academic_period_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"Période 2\",\"startsOn\":\"2027-01-05\",\"endsOn\":\"2027-03-20\"}"))
			.andExpect(status().isCreated());

		String periodsResponse = mockMvc.perform(get(periodUrl).with(administrator("can_academic_period_read")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String firstPeriodId = JsonPath.read(periodsResponse, "$[0].id");

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
			.patch(yearUrl + "/" + schoolYearId)
			.with(administrator("can_academic_year_update"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"Année 2026-2027\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Année 2026-2027"));

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
			.delete(periodUrl + "/" + firstPeriodId)
			.with(administrator("can_academic_period_delete")))
			.andExpect(status().isNoContent());

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
			.delete(yearUrl + "/" + schoolYearId)
			.with(administrator("can_academic_year_delete")))
			.andExpect(status().isNoContent());

		mockMvc.perform(get(yearUrl).with(administrator("can_academic_year_read")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(0)));
 	}

	@Test
	void rejectsSchoolPeriodOutsideItsYearAndUserWithoutPermission() throws Exception {
		Establishment establishment = createEstablishment();
		String yearUrl = yearUrl(establishment.id());
		String yearResponse = mockMvc.perform(post(yearUrl)
			.with(administrator("can_academic_year_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"2027-2028\",\"startsOn\":\"2027-09-01\",\"endsOn\":\"2028-06-30\"}"))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String schoolYearId = JsonPath.read(yearResponse, "$.id");

		mockMvc.perform(post(yearUrl + "/" + schoolYearId + "/periods")
			.with(administrator("can_academic_period_create"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"Avant la rentrée\",\"startsOn\":\"2027-08-15\",\"endsOn\":\"2027-09-15\"}"))
			.andExpect(status().isBadRequest());

		mockMvc.perform(get(yearUrl)
			.with(user("teacher@example.org").authorities(new SimpleGrantedAuthority("STAFF_ACCESS"))))
			.andExpect(status().isForbidden());
	}

	@Test
	void administratorWithGuardianPermissionStillReadsYearsWithoutLinkedChildren() throws Exception {
		Establishment establishment = createEstablishment();
		mockMvc.perform(get(yearUrl(establishment.id()))
			.with(user("admin-tech").authorities(new SimpleGrantedAuthority("ADMIN_ACCESS"),
				new SimpleGrantedAuthority("can_academic_year_read"),
				new SimpleGrantedAuthority("can_guardian_read_children"))))
			.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
	}

	private Establishment createEstablishment() {
		String code = "CAL-" + UUID.randomUUID().toString().substring(0, 8);
		return LicensedEstablishmentFixture.create(establishmentRepository, licenseApplicationService,
			code, "Calendar Test School");
	}

	private static String yearUrl(UUID establishmentId) {
		return "/api/v1/establishments/" + establishmentId + "/school-years";
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor administrator(String permission) {
		return user("admin-tech").authorities(
			new SimpleGrantedAuthority("ADMIN_ACCESS"), new SimpleGrantedAuthority(permission));
	}
}
