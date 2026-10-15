package com.galactique.gestionecole.tuition.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TuitionPaymentMethodJpaRepository extends JpaRepository<TuitionPaymentMethodEntity, UUID> {
	List<TuitionPaymentMethodEntity> findAllByEstablishmentIdAndEnabledTrueOrderByNameAsc(UUID establishmentId);
	List<TuitionPaymentMethodEntity> findAllByEstablishmentIdOrderByNameAsc(UUID establishmentId);
	java.util.Optional<TuitionPaymentMethodEntity> findByEstablishmentIdAndCode(UUID establishmentId, String code);
	java.util.Optional<TuitionPaymentMethodEntity> findByEstablishmentIdAndCodeAndEnabledTrue(UUID establishmentId, String code);
	boolean existsByEstablishmentIdAndCodeAndEnabledTrue(UUID establishmentId, String code);
}