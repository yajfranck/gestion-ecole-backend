package com.galactique.gestionecole.tuition.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TuitionPaymentJpaRepository extends JpaRepository<TuitionPaymentEntity, UUID> {
	@Query(value = "select nextval('tuition_receipt_sequence')", nativeQuery = true)
	long nextReceiptNumber();

	@Query("select coalesce(sum(payment.amount), 0) from TuitionPaymentEntity payment where payment.chargeId = :chargeId")
	BigDecimal sumAmountByChargeId(@Param("chargeId") UUID chargeId);
	List<TuitionPaymentEntity> findAllByChargeIdOrderByReceivedAtAsc(UUID chargeId);
	List<TuitionPaymentEntity> findAllByChargeIdInOrderByReceivedAtAsc(List<UUID> chargeIds);
	List<TuitionPaymentEntity> findAllByChargeIdAndEstablishmentIdAndStudentIdAndReceivedAtBetweenOrderByReceivedAtAsc(
		UUID chargeId, UUID establishmentId, UUID studentId, Instant from, Instant to);
	Optional<TuitionPaymentEntity> findByIdAndEstablishmentId(UUID id, UUID establishmentId);
	@Query("select p from TuitionPaymentEntity p, StudentFeeChargeEntity c where p.chargeId = c.id and p.id = :id and p.establishmentId = :school and c.academicYearId = :year and c.establishmentId = :school")
	Optional<TuitionPaymentEntity> findInYear(@Param("id") UUID id, @Param("school") UUID school, @Param("year") UUID year);
}
