package com.galactique.gestionecole.licenses.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LicenseEventJpaRepository extends JpaRepository<LicenseEventEntity, Long> {
	List<LicenseEventEntity> findAllByLicenseIdOrderByCreatedAtAsc(Long licenseId);
}
