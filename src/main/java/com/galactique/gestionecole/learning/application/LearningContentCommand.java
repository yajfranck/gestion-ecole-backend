package com.galactique.gestionecole.learning.application;

import java.time.LocalDate;
import java.util.UUID;

import com.galactique.gestionecole.learning.domain.LearningContentType;

public record LearningContentCommand(LearningContentType type, String title, String description,
	String body, UUID subjectId, LocalDate dueDate) {
}
