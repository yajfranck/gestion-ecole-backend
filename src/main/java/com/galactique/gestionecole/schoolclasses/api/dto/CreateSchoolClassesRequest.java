package com.galactique.gestionecole.schoolclasses.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSchoolClassesRequest(
	@NotEmpty @Size(max = 100) List<@NotNull @Valid CreateSchoolClassRequest> classes) { }