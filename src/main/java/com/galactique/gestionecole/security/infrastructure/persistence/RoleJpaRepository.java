package com.galactique.gestionecole.security.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleJpaRepository extends JpaRepository<RoleEntity, UUID> {
	@Override
	@EntityGraph(attributePaths = "permissions")
	List<RoleEntity> findAll();

	@EntityGraph(attributePaths = "permissions")
	java.util.Optional<RoleEntity> findByCode(String code);

	@EntityGraph(attributePaths = "permissions")
	java.util.Optional<RoleEntity> findByCodeAndEstablishmentId(String code, UUID establishmentId);

	@Query("select distinct role from RoleEntity role where role.establishmentId is null "
		+ "or role.establishmentId = :establishmentId order by role.code")
	@EntityGraph(attributePaths = "permissions")
	List<RoleEntity> findAllForEstablishment(@Param("establishmentId") UUID establishmentId);
}