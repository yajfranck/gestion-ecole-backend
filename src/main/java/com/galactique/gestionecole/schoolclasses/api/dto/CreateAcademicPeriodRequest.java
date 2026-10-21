package com.galactique.gestionecole.schoolclasses.api.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAcademicPeriodRequest(
	@NotBlank @Size(max = 100) String name,
	@NotNull LocalDate startsOn,
	@NotNull LocalDate endsOn) {
}