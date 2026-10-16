package com.galactique.gestionecole.licenses.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LicenseOfferJpaRepository extends JpaRepository<LicenseOfferEntity, UUID> {
	@EntityGraph(attributePaths = "features")
	List<LicenseOfferEntity> findAllByEnabledTrueOrderByCodeAsc();

	@EntityGraph(attributePaths = "features")
	Optional<LicenseOfferEntity> findByIdAndEnabledTrue(UUID id);

	boolean existsByCode(String code);
}
