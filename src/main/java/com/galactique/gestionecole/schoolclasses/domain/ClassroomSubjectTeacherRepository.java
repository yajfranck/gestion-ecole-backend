package com.galactique.gestionecole.schoolclasses.domain;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ClassroomSubjectTeacherRepository {
	Map<UUID, List<ClassroomSubjectTeacher>> findAllByClass(UUID establishmentId, UUID schoolClassId);

	void replace(UUID establishmentId, UUID academicYearId, UUID schoolClassId, UUID subjectId,
		List<ClassroomSubjectTeacher> teachers);
}
