package com.galactique.gestionecole.students.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StudentJpaRepository extends JpaRepository<StudentEntity, UUID> {
	boolean existsByEstablishmentIdAndStudentNumber(UUID establishmentId, String studentNumber);

	@Query(value = "select nextval('student_number_sequence')", nativeQuery = true)
	long nextStudentNumber();

	List<StudentEntity> findAllByEstablishmentIdAndEnabledTrueOrderByLastNameAscFirstNameAscStudentNumberAsc(
		UUID establishmentId);

	Optional<StudentEntity> findByIdAndEstablishmentIdAndEnabledTrue(UUID id, UUID establishmentId);
}