package com.galactique.gestionecole.guardians.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AcceptGuardianInvitationRequest(@NotBlank String token, @NotBlank String password) {
}