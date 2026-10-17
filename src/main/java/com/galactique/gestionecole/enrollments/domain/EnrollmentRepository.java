package com.galactique.gestionecole.enrollments.domain;

import java.util.List;
import java.util.UUID;

public interface EnrollmentRepository {
	Enrollment save(UUID establishmentId, UUID studentId, UUID academicYearId, UUID schoolClassId, UUID classroomId,
		boolean advanceAccepted, String paymentDecisionBy);

	java.util.Optional<Enrollment> findById(UUID establishmentId, UUID academicYearId, UUID enrollmentId);

	Enrollment updateClassroom(Enrollment enrollment, UUID classroomId);

	boolean existsByStudentIdAndAcademicYearId(UUID studentId, UUID academicYearId);

	List<Enrollment> findAllByEstablishmentIdAndAcademicYearId(UUID establishmentId, UUID academicYearId);

	List<Enrollment> findAllByEstablishmentIdAndAcademicYearIdAndSchoolClassId(
		UUID establishmentId, UUID academicYearId, UUID schoolClassId);

	java.util.Optional<Enrollment> findByEstablishmentIdAndAcademicYearIdAndStudentId(
		UUID establishmentId, UUID academicYearId, UUID studentId);

}
