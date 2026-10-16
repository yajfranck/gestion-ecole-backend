package com.galactique.gestionecole.learning.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LearningContent(UUID id, UUID establishmentId, UUID academicYearId, UUID schoolClassId,
	UUID subjectId, LearningContentType type, String title, String description, String body,
	LocalDate dueDate, LearningContentStatus status, String createdBy, String updatedBy,
	String publishedBy, Instant publishedAt, Instant createdAt, Instant updatedAt) {
}
