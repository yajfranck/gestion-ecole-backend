package com.galactique.gestionecole.guardians.api.dto;

import java.util.UUID;

import com.galactique.gestionecole.guardians.domain.Guardian;

public record GuardianResponse(UUID id, String firstName, String lastName, String email) {
	public static GuardianResponse from(Guardian guardian) {
		return new GuardianResponse(guardian.id(), guardian.firstName(), guardian.lastName(), guardian.email());
	}
}