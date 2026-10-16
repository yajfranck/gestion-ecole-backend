package com.galactique.gestionecole.learning.domain;

import java.time.LocalDate;
import java.util.UUID;

public record LearningContentDraft(UUID establishmentId, UUID academicYearId, UUID schoolClassId,
	UUID subjectId, LearningContentType type, String title, String description, String body,
	LocalDate dueDate, String username) {
}
