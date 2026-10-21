package com.galactique.gestionecole.schoolclasses.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.galactique.gestionecole.schoolclasses.domain.ClassroomSubjectTeacher;
import com.galactique.gestionecole.schoolclasses.domain.SchoolClassSubject;

public record SchoolClassSubjectResponse(UUID id, UUID establishmentId, UUID schoolClassId, UUID subjectId,
	boolean enabled, Instant createdAt, Instant updatedAt,
	java.util.List<ClassroomSubjectTeacher> classroomTeachers) {
	public static SchoolClassSubjectResponse from(SchoolClassSubject assignment) {
		return new SchoolClassSubjectResponse(assignment.id(), assignment.establishmentId(), assignment.schoolClassId(),
			assignment.subjectId(), assignment.enabled(), assignment.createdAt(), assignment.updatedAt(), java.util.List.of());
	}
	public static SchoolClassSubjectResponse from(SchoolClassSubject assignment,
		java.util.List<ClassroomSubjectTeacher> teachers) {
		return new SchoolClassSubjectResponse(assignment.id(), assignment.establishmentId(), assignment.schoolClassId(),
			assignment.subjectId(), assignment.enabled(), assignment.createdAt(), assignment.updatedAt(), teachers);
	}

}
