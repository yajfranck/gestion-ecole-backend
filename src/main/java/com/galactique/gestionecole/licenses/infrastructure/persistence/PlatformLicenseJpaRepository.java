package com.galactique.gestionecole.licenses.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PlatformLicenseJpaRepository extends JpaRepository<PlatformLicenseEntity, Long> {
	@EntityGraph(attributePaths = "offer.features")
	List<PlatformLicenseEntity> findAllByEstablishmentIdOrderByValidFromDesc(UUID establishmentId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = "offer.features")
	@Query("select license from PlatformLicenseEntity license where license.establishmentId = :establishmentId "
		+ "order by license.validFrom desc")
	List<PlatformLicenseEntity> findAllForUpdate(@Param("establishmentId") UUID establishmentId);

	@EntityGraph(attributePaths = "offer.features")
	Optional<PlatformLicenseEntity> findByIdAndEstablishmentId(Long id, UUID establishmentId);
}
