package com.galactique.gestionecole.establishments.api.dto;

import java.util.UUID;

import com.galactique.gestionecole.establishments.api.EstablishmentResponse;
import com.galactique.gestionecole.establishments.application.OnboardedEstablishment;

public record EstablishmentOnboardingResponse(
	EstablishmentResponse establishment,
	AdministratorInvitation administratorInvitation) {
	public static EstablishmentOnboardingResponse from(OnboardedEstablishment onboarded) {
		return new EstablishmentOnboardingResponse(EstablishmentResponse.from(onboarded.establishment()),
			new AdministratorInvitation(onboarded.administratorEmployeeId(), "INVITATION_SENT"));
	}

	public record AdministratorInvitation(UUID employeeId, String status) {
	}
}