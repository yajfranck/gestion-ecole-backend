package com.galactique.gestionecole.schoolclasses.api.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAcademicYearRequest(
	@NotBlank @Size(max = 100) String name,
	LocalDate startsOn,
	LocalDate endsOn) {
	public boolean hasChanges() {
		return name != null || startsOn != null || endsOn != null;
	}
}