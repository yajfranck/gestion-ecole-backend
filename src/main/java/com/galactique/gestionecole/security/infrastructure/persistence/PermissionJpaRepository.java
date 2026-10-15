package com.galactique.gestionecole.security.infrastructure.persistence;

import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionJpaRepository extends JpaRepository<PermissionEntity, UUID> {
	List<PermissionEntity> findAllByCodeIn(List<String> codes);
}