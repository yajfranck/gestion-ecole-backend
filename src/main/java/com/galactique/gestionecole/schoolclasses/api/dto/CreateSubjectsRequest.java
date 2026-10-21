package com.galactique.gestionecole.schoolclasses.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSubjectsRequest(
	@NotNull @Size(min = 1, max = 100) List<@NotNull @Valid CreateSubjectRequest> subjects) {
}
