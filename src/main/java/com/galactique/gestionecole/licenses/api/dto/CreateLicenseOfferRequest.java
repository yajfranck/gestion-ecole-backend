package com.galactique.gestionecole.licenses.api.dto;

import java.util.Set;

import com.galactique.gestionecole.licenses.domain.LicenseFeature;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateLicenseOfferRequest(@NotBlank @Size(max = 40) String code,
	@NotBlank @Size(max = 100) String name, @NotEmpty Set<LicenseFeature> features) {
}
