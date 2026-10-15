package com.galactique.gestionecole.tuition.infrastructure.persistence;

import java.math.BigDecimal;
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
	Optional<TuitionPaymentEntity> findByIdAndEstablishmentId(UUID id, UUID establishmentId);
}