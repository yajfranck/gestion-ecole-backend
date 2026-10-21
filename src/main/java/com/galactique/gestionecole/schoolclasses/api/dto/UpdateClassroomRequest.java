package com.galactique.gestionecole.schoolclasses.api.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateClassroomRequest(@Pattern(regexp = "(?sU).*\\S.*") @Size(max = 40) String code,
	@Pattern(regexp = "(?sU).*\\S.*") @Size(max = 100) String name, @Min(1) Integer maxStudents) { }
