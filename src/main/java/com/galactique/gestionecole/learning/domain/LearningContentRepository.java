package com.galactique.gestionecole.learning.domain;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface LearningContentRepository {
	LearningContent save(LearningContentDraft draft);

	LearningContentPage findPage(UUID establishmentId, UUID academicYearId, UUID schoolClassId,
		LearningContentType type, boolean publishedOnly, int page, int size);

	Optional<LearningContent> find(UUID establishmentId, UUID academicYearId, UUID schoolClassId, UUID contentId);

	LearningContent update(LearningContent current, String title, String description, String body,
		UUID subjectId, LocalDate dueDate, String username);

	LearningContent publish(LearningContent current, String username);

	LearningContent archive(LearningContent current, String username);
}
