package com.galactique.gestionecole.schoolclasses.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

public interface ClassroomJpaRepository extends JpaRepository<ClassroomEntity, UUID> {
	List<ClassroomEntity> findAllByEstablishmentIdAndAcademicYearIdAndSchoolClassIdAndEnabledTrueOrderByNameAscIdAsc(
		UUID establishmentId, UUID academicYearId, UUID schoolClassId);
	Optional<ClassroomEntity> findByIdAndEstablishmentIdAndAcademicYearIdAndSchoolClassIdAndEnabledTrue(
		UUID id, UUID establishmentId, UUID academicYearId, UUID schoolClassId);
	@Query(value = "SELECT COUNT(*) FROM enrollments WHERE classroom_id = :classroomId", nativeQuery = true)
	long countEnrollments(UUID classroomId);
	@Query(value = "SELECT c.school_class_id AS schoolClassId, COUNT(c.id) AS classroomCount, "
		+ "COALESCE(SUM(c.max_students), 0) AS totalCapacity, "
		+ "(SELECT COUNT(e.id) FROM enrollments e WHERE e.establishment_id = c.establishment_id "
		+ "AND e.academic_year_id = c.academic_year_id AND e.school_class_id = c.school_class_id) "
		+ "AS enrolledStudentCount FROM classrooms c WHERE c.establishment_id = :establishmentId "
		+ "AND c.academic_year_id = :academicYearId AND c.enabled = TRUE "
		+ "GROUP BY c.establishment_id, c.academic_year_id, c.school_class_id", nativeQuery = true)
	List<ClassroomStatisticsProjection> findStatisticsByEstablishmentIdAndAcademicYearId(
		@Param("establishmentId") UUID establishmentId, @Param("academicYearId") UUID academicYearId);
	@Query(value = "SELECT COUNT(*) FROM timetable_entries WHERE classroom_id = :classroomId AND enabled = TRUE", nativeQuery = true)
	long countTimetableEntries(UUID classroomId);
	Optional<ClassroomEntity> findByIdAndEstablishmentIdAndAcademicYearIdAndEnabledTrue(
		UUID id, UUID establishmentId, UUID academicYearId);
}
