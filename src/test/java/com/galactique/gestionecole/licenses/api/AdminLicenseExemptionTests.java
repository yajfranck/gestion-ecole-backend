package com.galactique.gestionecole.licenses.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.galactique.gestionecole.employees.domain.EmployeeRepository;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;
import com.galactique.gestionecole.licenses.domain.LicenseFeature;
import com.galactique.gestionecole.positions.domain.PositionRepository;
import com.galactique.gestionecole.schoolclasses.domain.AcademicYearRepository;
import com.galactique.gestionecole.schoolclasses.domain.SchoolClassRepository;
import com.galactique.gestionecole.security.domain.SecurityAccountProvisioner;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AdminLicenseExemptionTests {
	@Autowired MockMvc mvc;
	@Autowired JdbcTemplate jdbc;
	@Autowired EstablishmentRepository schools;
	@Autowired AcademicYearRepository years;
	@Autowired SchoolClassRepository classes;
	@Autowired LicenseApplicationService licenses;
	@Autowired EmployeeRepository employees;
	@Autowired PositionRepository positions;
	@Autowired SecurityAccountProvisioner accounts;

	@Test
	void adminRequiresActiveLicensesAcrossModules() throws Exception {
		for (String state : List.of("UNLICENSED", "SUSPENDED", "EXPIRED", "UPCOMING", "OFFER_DISABLED", "MISSING_FEATURE")) {
			var f = fixture(state);
			var admin = as("admin-tech", "ROLE_ADMIN", "ADMIN_ACCESS", "can_student_read", "can_academic_year_read",
				"can_school_class_read", "can_subject_read", "can_enrollment_read", "can_tuition_read", "can_learning_content_read");
			for (String url : List.of(f.base() + "/students", f.base() + "/school-years", f.yearUrl() + "/classes",
				f.base() + "/subjects", f.yearUrl() + "/enrollments", f.yearUrl() + "/tuition-fees",
				f.yearUrl() + "/classes/" + f.schoolClass() + "/learning-content")) {
				mvc.perform(get(url).with(admin)).andExpect(status().isForbidden());
			}
			assertFalse(licenses.hasFeature(f.school(), LicenseFeature.STUDENT_RECORDS, Instant.now()));
			if (!state.equals("UNLICENSED")) {
				mvc.perform(get(f.base() + "/license").with(as("admin-tech", "can_license_read"))).andExpect(status().isOk())
					.andExpect(jsonPath("$[0].state").value((state.equals("MISSING_FEATURE") || state.equals("OFFER_DISABLED")) ? "ACTIVE" : state));
			}
		}
	}

	@Test
	void establishmentAdminRemainsRestrictedAndStaleAdminClaimsDoNotBypassLicenses() throws Exception {
		for (String state : List.of("UNLICENSED", "SUSPENDED", "EXPIRED", "UPCOMING", "OFFER_DISABLED", "MISSING_FEATURE")) {
			var f = fixture(state);
			String email = schoolAdmin(f.school());
			mvc.perform(get(f.base() + "/students").with(as(email, "ROLE_ESTABLISHMENT_ADMIN",
				"ESTABLISHMENT_STAFF_MANAGEMENT", "can_student_read"))).andExpect(status().isForbidden());
			mvc.perform(get(f.base() + "/students").with(as(email, "ROLE_ADMIN", "ADMIN_ACCESS", "can_student_read")))
				.andExpect(status().isForbidden());
		}
	}

	@Test
	void exemptionDoesNotGrantMissingEndpointPermissionsOrExposeDisabledEstablishments() throws Exception {
		var f = fixture("UNLICENSED");
		mvc.perform(get(f.base() + "/students").with(as("admin-tech", "ROLE_ADMIN", "ADMIN_ACCESS")))
			.andExpect(status().isForbidden());
		mvc.perform(get(f.base() + "/students").with(as("admin-tech", "ROLE_ADMIN", "can_student_read")))
			.andExpect(status().isForbidden());
		jdbc.update("UPDATE establishments SET enabled=FALSE WHERE id=?", f.school());
		mvc.perform(get(f.base() + "/students").with(as("admin-tech", "ROLE_ADMIN", "ADMIN_ACCESS", "can_student_read")))
			.andExpect(status().isForbidden());
	}

	private Fixture fixture(String state) {
		var school = schools.save("EXEMPT-" + UUID.randomUUID().toString().substring(0, 8), "License exemption school");
		var year = years.save(school.id(), "2026-2027", LocalDate.of(2026, 9, 1), LocalDate.of(2027, 6, 30));
		var schoolClass = classes.save(school.id(), year.id(), "6", "Sixième", null);
		if (!state.equals("UNLICENSED")) {
			var offer = licenses.createOffer("EXEMPT-" + UUID.randomUUID().toString().substring(0, 8), "Parent only", Set.of(LicenseFeature.PARENT_PORTAL));
			var license = licenses.assign(school.id(), offer.id(), state.equals("UPCOMING") ? Instant.now().plusSeconds(3600) : null, null, "admin-tech");
			if (state.equals("OFFER_DISABLED")) { jdbc.update("UPDATE license_offers SET enabled=FALSE WHERE id=?", offer.id()); }
			if (state.equals("SUSPENDED")) { licenses.suspend(school.id(), license.id(), "admin-tech"); }
			if (state.equals("EXPIRED")) {
				jdbc.update("UPDATE platform_licenses SET valid_from=DATE '2020-01-01', valid_until=DATE '2021-01-01' WHERE id=?", license.id());
			}
		}
		return new Fixture(school.id(), year.id(), schoolClass.id());
	}
	private String schoolAdmin(UUID school) {
		var position = positions.save(school, "Administrator", Set.of("ESTABLISHMENT_ADMIN"));
		String email = "license-exemption-" + UUID.randomUUID() + "@example.org";
		var employee = employees.save("EX-" + UUID.randomUUID(), "School", "Admin", email, position.id(), Set.of());
		employees.attachUserAccount(employee.id(), accounts.createEmployeeAccount(email, "license-exemption-password", Set.of("ESTABLISHMENT_ADMIN")));
		return email;
	}
	private RequestPostProcessor as(String username, String... permissions) {
		return user(username).authorities(Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList());
	}
	private record Fixture(UUID school, UUID year, UUID schoolClass) {
		String base() { return "/api/v1/establishments/" + school; }
		String yearUrl() { return base() + "/school-years/" + year; }
	}
}
