package com.galactique.gestionecole.enrollments.domain;

import java.util.List;
import java.util.UUID;

public interface EnrollmentRepository {
	Enrollment save(UUID establishmentId, UUID studentId, UUID academicYearId, UUID schoolClassId);

	boolean existsByStudentIdAndAcademicYearId(UUID studentId, UUID academicYearId);

	List<Enrollment> findAllByEstablishmentIdAndAcademicYearId(UUID establishmentId, UUID academicYearId);

}