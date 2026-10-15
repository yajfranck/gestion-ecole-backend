package com.galactique.gestionecole.security.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleJpaRepository extends JpaRepository<RoleEntity, UUID> {
	@Override
	@EntityGraph(attributePaths = "permissions")
	List<RoleEntity> findAll();

	@EntityGraph(attributePaths = "permissions")
	java.util.Optional<RoleEntity> findByCode(String code);
}