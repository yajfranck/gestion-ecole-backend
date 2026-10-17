package com.galactique.gestionecole.learning.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.galactique.gestionecole.learning.application.LearningContentCommand;
import com.galactique.gestionecole.learning.domain.LearningContentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LearningContentRequest(@NotNull LearningContentType type, @NotBlank @Size(max = 160) String title,
	@Size(max = 2000) String description, @Size(max = 100000) String body,
	@NotNull UUID subjectId, LocalDate dueDate) {
	public LearningContentCommand toCommand() {
		return new LearningContentCommand(type, title, description, body, subjectId, dueDate);
	}
}
