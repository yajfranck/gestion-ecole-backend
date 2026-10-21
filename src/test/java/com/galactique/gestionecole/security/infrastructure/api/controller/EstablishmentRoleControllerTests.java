package com.galactique.gestionecole.security.infrastructure.api.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.galactique.gestionecole.employees.domain.EmployeeRepository;
import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.positions.domain.PositionRepository;
import com.galactique.gestionecole.security.domain.SecurityAccountProvisioner;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class EstablishmentRoleControllerTests {
	private final MockMvc mockMvc;
	private final EstablishmentRepository establishmentRepository;
	private final PositionRepository positionRepository;
	private final EmployeeRepository employeeRepository;
	private final SecurityAccountProvisioner securityAccountProvisioner;

	@Autowired
	EstablishmentRoleControllerTests(MockMvc mockMvc, EstablishmentRepository establishmentRepository,
		PositionRepository positionRepository, EmployeeRepository employeeRepository,
		SecurityAccountProvisioner securityAccountProvisioner) {
		this.mockMvc = mockMvc;
		this.establishmentRepository = establishmentRepository;
		this.positionRepository = positionRepository;
		this.employeeRepository = employeeRepository;
		this.securityAccountProvisioner = securityAccountProvisioner;
	}

	@Test
	void establishmentAdministratorCanManageLocalRolesAndReadAssignablePermissions() throws Exception {
		SchoolAdmin schoolAdmin = createSchoolAdmin();
		String rolesUrl = "/api/v1/establishments/" + schoolAdmin.establishment().id() + "/roles";
		String roleCode = uniqueRoleCode();

		mockMvc.perform(get(rolesUrl).with(schoolAdmin(schoolAdmin.email())))
			.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/establishments/" + schoolAdmin.establishment().id() + "/permissions")
			.with(schoolAdmin(schoolAdmin.email())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].code", hasItem("can_student_read")))
			.andExpect(jsonPath("$[*].code", not(hasItem("ADMIN_ACCESS"))))
			.andExpect(jsonPath("$[*].code", hasItem("can_establishment_read")))
			.andExpect(jsonPath("$[*].code", hasItem("can_establishment_onboard")));

		mockMvc.perform(post(rolesUrl)
			.with(schoolAdmin(schoolAdmin.email()))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"" + roleCode + "\",\"name\":\"School Reader\","
				+ "\"permissionCodes\":[\"can_student_read\"]}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.establishmentId").value(schoolAdmin.establishment().id().toString()))
			.andExpect(jsonPath("$.editable").value(true));

		mockMvc.perform(put(rolesUrl + "/" + roleCode)
			.with(schoolAdmin(schoolAdmin.email()))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"School Enrollment Reader\","
				+ "\"permissionCodes\":[\"can_enrollment_read\"]}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.permissions", hasSize(1)))
			.andExpect(jsonPath("$.permissions[0].code").value("can_enrollment_read"));

		mockMvc.perform(post(rolesUrl)
			.with(schoolAdmin(schoolAdmin.email()))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"PLATFORM_GRANT_" + roleCode + "\",\"name\":\"Platform Grant\","
				+ "\"permissionCodes\":[\"ADMIN_ACCESS\"]}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void establishmentAdministratorCannotReadOrEditAnotherEstablishmentsRoles() throws Exception {
		SchoolAdmin firstAdmin = createSchoolAdmin();
		SchoolAdmin secondAdmin = createSchoolAdmin();
		String secondRolesUrl = "/api/v1/establishments/" + secondAdmin.establishment().id() + "/roles";
		String roleCode = uniqueRoleCode();

		mockMvc.perform(post(secondRolesUrl)
			.with(schoolAdmin(secondAdmin.email()))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"" + roleCode + "\",\"name\":\"Second School Role\","
				+ "\"permissionCodes\":[\"can_student_read\"]}"))
			.andExpect(status().isCreated());

		mockMvc.perform(get(secondRolesUrl).with(schoolAdmin(firstAdmin.email())))
			.andExpect(status().isForbidden());

		mockMvc.perform(put(secondRolesUrl + "/" + roleCode)
			.with(schoolAdmin(firstAdmin.email()))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"Hijacked Role\",\"permissionCodes\":[\"can_student_read\"]}"))
			.andExpect(status().isForbidden());
	}

	private SchoolAdmin createSchoolAdmin() {
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
		Establishment establishment = establishmentRepository.save("ROLE-" + suffix, "Role Management School");
		var position = positionRepository.save(establishment.id(), "School Administrator",
			Set.of("ESTABLISHMENT_ADMIN"));
		String email = "school-admin-" + suffix.toLowerCase() + "@example.org";
		var employee = employeeRepository.save("ROLE-ADMIN-" + suffix, "School", "Admin", email,
			position.id(), Set.of());
		UUID userAccountId = securityAccountProvisioner.createEmployeeAccount(email,
			"TemporaryTestPassword123!", Set.of("ESTABLISHMENT_ADMIN"));
		employeeRepository.attachUserAccount(employee.id(), userAccountId);
		return new SchoolAdmin(establishment, email);
	}

	private static RequestPostProcessor schoolAdmin(String email) {
		var authorities = new ArrayList<SimpleGrantedAuthority>();
		authorities.add(new SimpleGrantedAuthority("ESTABLISHMENT_STAFF_MANAGEMENT"));
		authorities.add(new SimpleGrantedAuthority("can_role_read"));
		authorities.add(new SimpleGrantedAuthority("can_role_create"));
		authorities.add(new SimpleGrantedAuthority("can_role_update"));
		authorities.add(new SimpleGrantedAuthority("can_permission_read"));
		return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
			.user(email).authorities(authorities.toArray(SimpleGrantedAuthority[]::new));
	}

	private static String uniqueRoleCode() {
		return "SCHOOL_ROLE_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
	}

	private record SchoolAdmin(Establishment establishment, String email) {
	}
}
