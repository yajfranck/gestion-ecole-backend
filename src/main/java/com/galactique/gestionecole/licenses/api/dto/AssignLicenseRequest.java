package com.galactique.gestionecole.licenses.api.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AssignLicenseRequest(@NotNull UUID offerId, Instant validFrom, Instant validUntil) {
}
