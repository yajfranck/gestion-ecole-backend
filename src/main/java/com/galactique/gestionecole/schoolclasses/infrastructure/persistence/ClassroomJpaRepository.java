package com.galactique.gestionecole.schoolclasses.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClassroomJpaRepository extends JpaRepository<ClassroomEntity, UUID> {
	List<ClassroomEntity> findAllByEstablishmentIdAndAcademicYearIdAndSchoolClassIdAndEnabledTrueOrderByNameAscIdAsc(
		UUID establishmentId, UUID academicYearId, UUID schoolClassId);
	Optional<ClassroomEntity> findByIdAndEstablishmentIdAndAcademicYearIdAndSchoolClassIdAndEnabledTrue(
		UUID id, UUID establishmentId, UUID academicYearId, UUID schoolClassId);
	@Query(value = "SELECT COUNT(*) FROM enrollments WHERE classroom_id = :classroomId", nativeQuery = true)
	long countEnrollments(UUID classroomId);
	@Query(value = "SELECT COUNT(*) FROM timetable_entries WHERE classroom_id = :classroomId AND enabled = TRUE", nativeQuery = true)
	long countTimetableEntries(UUID classroomId);
	Optional<ClassroomEntity> findByIdAndEstablishmentIdAndAcademicYearIdAndEnabledTrue(
		UUID id, UUID establishmentId, UUID academicYearId);
}
