package com.galactique.gestionecole.schoolclasses.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateClassroomRequest(@NotBlank @Size(max = 40) String code,
	@NotBlank @Size(max = 100) String name, @NotNull @Min(1) Integer maxStudents) { }
