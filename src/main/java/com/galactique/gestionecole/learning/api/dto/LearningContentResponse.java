package com.galactique.gestionecole.learning.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.galactique.gestionecole.learning.domain.LearningContent;
import com.galactique.gestionecole.learning.domain.LearningContentStatus;
import com.galactique.gestionecole.learning.domain.LearningContentType;

public record LearningContentResponse(UUID id, UUID establishmentId, UUID academicYearId,
	UUID schoolClassId, UUID subjectId, LearningContentType type, String title, String description,
	String body, LocalDate dueDate, LearningContentStatus status, Instant publishedAt,
	Instant createdAt, Instant updatedAt) {
	public static LearningContentResponse from(LearningContent content) {
		return new LearningContentResponse(content.id(), content.establishmentId(), content.academicYearId(),
			content.schoolClassId(), content.subjectId(), content.type(), content.title(), content.description(),
			content.body(), content.dueDate(), content.status(), content.publishedAt(),
			content.createdAt(), content.updatedAt());
	}
}
