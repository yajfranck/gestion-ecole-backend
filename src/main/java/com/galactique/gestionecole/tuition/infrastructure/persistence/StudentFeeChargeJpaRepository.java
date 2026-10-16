package com.galactique.gestionecole.tuition.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface StudentFeeChargeJpaRepository extends JpaRepository<StudentFeeChargeEntity, UUID> {
	boolean existsByFeeIdAndStudentId(UUID feeId, UUID studentId);
	List<StudentFeeChargeEntity> findAllByEstablishmentIdAndStudentIdOrderByDueDateAsc(UUID establishmentId, UUID studentId);
	List<StudentFeeChargeEntity> findAllByEstablishmentIdAndStudentIdAndAcademicYearIdOrderByDueDateAsc(
		UUID establishmentId, UUID studentId, UUID academicYearId);
	List<StudentFeeChargeEntity> findAllByEstablishmentIdAndAcademicYearIdOrderByDueDateAsc(UUID establishmentId, UUID academicYearId);
	Optional<StudentFeeChargeEntity> findByIdAndEstablishmentIdAndStudentIdAndAcademicYearId(
		UUID id, UUID establishmentId, UUID studentId, UUID academicYearId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select charge from StudentFeeChargeEntity charge where charge.id = :chargeId "
		+ "and charge.establishmentId = :establishmentId and charge.studentId = :studentId")
	Optional<StudentFeeChargeEntity> findByIdForUpdate(@Param("chargeId") UUID chargeId,
		@Param("establishmentId") UUID establishmentId, @Param("studentId") UUID studentId);
}