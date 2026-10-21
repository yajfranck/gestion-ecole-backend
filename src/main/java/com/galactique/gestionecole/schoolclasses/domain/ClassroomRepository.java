package com.galactique.gestionecole.schoolclasses.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassroomRepository {
	Classroom save(UUID establishmentId, UUID academicYearId, UUID schoolClassId, String code, String name, int maxStudents);
	List<Classroom> findAll(UUID establishmentId, UUID academicYearId, UUID schoolClassId);
	Optional<Classroom> findById(UUID establishmentId, UUID academicYearId, UUID schoolClassId, UUID classroomId);
	Classroom update(Classroom current, String code, String name, int maxStudents);
	void archive(Classroom current);
	boolean hasEnrollments(UUID classroomId);
	long countEnrollments(UUID classroomId);
	boolean hasTimetableEntries(UUID classroomId);
	Optional<Classroom> findInYear(UUID establishmentId, UUID academicYearId, UUID classroomId);
}
