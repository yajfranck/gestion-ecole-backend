package com.galactique.gestionecole.establishments.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.galactique.gestionecole.employees.application.CreateEmployeeCommand;
import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.employees.application.EmployeeApplicationService;
import com.galactique.gestionecole.employees.application.EmployeeInvitationMailer;
import com.galactique.gestionecole.employees.domain.Employee;
import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.positions.application.PositionApplicationService;
import com.galactique.gestionecole.security.domain.UserAccountRepository;
import com.galactique.gestionecole.security.infrastructure.security.SecurityUserPrincipal;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class EstablishmentUpdateControllerTests {
	private static final String ADMIN_PASSWORD = "establishment-admin-password";
	private static final EmployeeActor GLOBAL_ADMIN = new EmployeeActor("admin-tech");

	private final MockMvc mockMvc;
	private final EstablishmentRepository establishmentRepository;
	private final PositionApplicationService positionApplicationService;
	private final EmployeeApplicationService employeeApplicationService;
	private final UserAccountRepository userAccountRepository;
	private final JdbcTemplate jdbcTemplate;

	@MockitoBean
	private EmployeeInvitationMailer invitationMailer;

	@Autowired
	EstablishmentUpdateControllerTests(MockMvc mockMvc, EstablishmentRepository establishmentRepository,
		PositionApplicationService positionApplicationService, EmployeeApplicationService employeeApplicationService,
		UserAccountRepository userAccountRepository, JdbcTemplate jdbcTemplate) {
		this.mockMvc = mockMvc;
		this.establishmentRepository = establishmentRepository;
		this.positionApplicationService = positionApplicationService;
		this.employeeApplicationService = employeeApplicationService;
		this.userAccountRepository = userAccountRepository;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Test
	void establishmentAdministratorCanPatchOnlyTheirOwnEstablishment() throws Exception {
		Establishment establishment = createEstablishment("LOCAL");
		Establishment otherEstablishment = createEstablishment("OTHER");
		String accessToken = createEstablishmentAdministrator(establishment);

		MvcResult updated = mockMvc.perform(patch(establishmentUrl(establishment.id()))
			.header("Authorization", "Bearer " + accessToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\" Updated School \"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(establishment.id().toString()))
			.andExpect(jsonPath("$.code").value(establishment.code()))
			.andExpect(jsonPath("$.name").value("Updated School"))
			.andExpect(jsonPath("$.enabled").value(true))
			.andReturn();
		assertEquals(establishment.createdAt().toString(),
			JsonPath.read(updated.getResponse().getContentAsString(), "$.createdAt"));
		assertNotEquals(establishment.updatedAt().toString(),
			JsonPath.read(updated.getResponse().getContentAsString(), "$.updatedAt"));

		mockMvc.perform(patch(establishmentUrl(otherEstablishment.id()))
			.header("Authorization", "Bearer " + accessToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"Unauthorized update\"}"))
			.andExpect(status().isForbidden());
	}

	@Test
	void platformAdminCanPatchWithoutAnUpdatePermissionAndInvalidValuesAreRejected() throws Exception {
		Establishment establishment = createEstablishment("PLATFORM");
		var adminAccount = userAccountRepository.findByUsername("admin-tech").orElseThrow();

		mockMvc.perform(patch(establishmentUrl(establishment.id()))
			.with(user(new SecurityUserPrincipal(adminAccount)))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"new-code\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("NEW-CODE"))
			.andExpect(jsonPath("$.name").value(establishment.name()));

		mockMvc.perform(patch(establishmentUrl(establishment.id()))
			.with(user(new SecurityUserPrincipal(adminAccount)))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":null}"))
			.andExpect(status().isBadRequest());
		mockMvc.perform(patch(establishmentUrl(establishment.id()))
			.with(user(new SecurityUserPrincipal(adminAccount)))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"   \"}"))
			.andExpect(status().isBadRequest());
		mockMvc.perform(patch(establishmentUrl(establishment.id()))
			.with(user(new SecurityUserPrincipal(adminAccount)))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"" + "C".repeat(41) + "\"}"))
			.andExpect(status().isBadRequest());
		mockMvc.perform(patch(establishmentUrl(establishment.id()))
			.with(user(new SecurityUserPrincipal(adminAccount)))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"" + "N".repeat(201) + "\"}"))
			.andExpect(status().isBadRequest());
		mockMvc.perform(patch(establishmentUrl(establishment.id()))
			.with(user(new SecurityUserPrincipal(adminAccount)))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{}"))
			.andExpect(status().isBadRequest());

		mockMvc.perform(patch(establishmentUrl(establishment.id()))
			.with(user("reader").authorities(new SimpleGrantedAuthority("can_establishment_read")))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"Read is not update\"}"))
			.andExpect(status().isForbidden());
	}

	@Test
	void duplicateEstablishmentCodeReturnsConflict() throws Exception {
		Establishment establishment = createEstablishment("FIRST");
		Establishment otherEstablishment = createEstablishment("SECOND");
		var adminAccount = userAccountRepository.findByUsername("admin-tech").orElseThrow();
		assertNotEquals(establishment.id(), otherEstablishment.id());
		assertEquals(1, establishmentRepository.findAll().stream()
			.filter(item -> item.code().equals(establishment.code())).count());
		assertEquals(establishment.code(), jdbcTemplate.queryForObject(
			"select code from establishments where id = ?", String.class, establishment.id()));
		assertTrue(jdbcTemplate.queryForObject("select count(*) from information_schema.table_constraints "
			+ "where table_name = 'establishments' and constraint_type = 'UNIQUE'", Integer.class) > 0);
		String duplicateCode = establishment.code();
		assertThrows(DataIntegrityViolationException.class,
			() -> establishmentRepository.save(duplicateCode, "Duplicate Code School"));

		mockMvc.perform(patch(establishmentUrl(otherEstablishment.id()))
			.with(user(new SecurityUserPrincipal(adminAccount)))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"code\":\"" + establishment.code() + "\"}"))
			.andExpect(status().isConflict());
	}

	private String createEstablishmentAdministrator(Establishment establishment) throws Exception {
		String email = "est-admin-" + UUID.randomUUID() + "@example.org";
		var position = positionApplicationService.create(establishment.id(), "School Administrator",
			Set.of("ESTABLISHMENT_ADMIN"), GLOBAL_ADMIN);
		Employee employee = employeeApplicationService.invite(new CreateEmployeeCommand(
			"ADM-" + UUID.randomUUID().toString().substring(0, 8), "School", "Admin", email,
			position.id(), Set.of()), GLOBAL_ADMIN);
		ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
		verify(invitationMailer).sendInvitation(eq(employee), token.capture());
		employeeApplicationService.acceptInvitation(token.getValue(), ADMIN_PASSWORD);

		MvcResult login = mockMvc.perform(post("/api/v1/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"login\":\"" + email + "\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
			.andExpect(status().isOk())
			.andReturn();
		return JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
	}

	private Establishment createEstablishment(String prefix) {
		return establishmentRepository.save(prefix + "-" + UUID.randomUUID().toString().substring(0, 8), prefix + " School");
	}

	private static String establishmentUrl(UUID establishmentId) {
		return "/api/v1/establishments/" + establishmentId;
	}
}
