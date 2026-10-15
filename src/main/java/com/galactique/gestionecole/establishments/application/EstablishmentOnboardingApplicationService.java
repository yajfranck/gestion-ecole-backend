package com.galactique.gestionecole.establishments.application;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.employees.application.CreateEmployeeCommand;
import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.employees.application.EmployeeApplicationService;
import com.galactique.gestionecole.establishments.api.dto.OnboardEstablishmentRequest;
import com.galactique.gestionecole.positions.application.PositionApplicationService;

@Service
public class EstablishmentOnboardingApplicationService {
	private static final String ADMINISTRATOR_POSITION_NAME = "School Administrator";

	private final EstablishmentApplicationService establishmentApplicationService;
	private final PositionApplicationService positionApplicationService;
	private final EmployeeApplicationService employeeApplicationService;

	public EstablishmentOnboardingApplicationService(
		EstablishmentApplicationService establishmentApplicationService,
		PositionApplicationService positionApplicationService,
		EmployeeApplicationService employeeApplicationService) {
		this.establishmentApplicationService = establishmentApplicationService;
		this.positionApplicationService = positionApplicationService;
		this.employeeApplicationService = employeeApplicationService;
	}

	@Transactional
	public OnboardedEstablishment onboard(OnboardEstablishmentRequest request, String initiatedBy) {
		var establishment = establishmentApplicationService.create(request.code(), request.name());
		EmployeeActor onboardingActor = new EmployeeActor(initiatedBy, true, false);
		var position = positionApplicationService.create(establishment.id(), ADMINISTRATOR_POSITION_NAME,
			Set.of("ESTABLISHMENT_ADMIN"), onboardingActor);
		var administrator = request.administrator();
		var employee = employeeApplicationService.invite(new CreateEmployeeCommand(
			administrator.employeeNumber(), administrator.firstName(), administrator.lastName(),
			administrator.email(), position.id(), Set.of()), onboardingActor);
		return new OnboardedEstablishment(establishment, employee.id());
	}
}