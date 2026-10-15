package com.galactique.gestionecole.security.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface UserAccountJpaRepository extends JpaRepository<UserAccountEntity, UUID> {
	@EntityGraph(attributePaths = {"roles", "roles.permissions"})
	Optional<UserAccountEntity> findByUsernameIgnoreCase(String username);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select account from UserAccountEntity account where lower(account.username) = lower(:username)")
	Optional<UserAccountEntity> findByUsernameForUpdate(@Param("username") String username);

	@Override
	@EntityGraph(attributePaths = {"roles", "roles.permissions"})
	List<UserAccountEntity> findAll();

	@EntityGraph(attributePaths = {"roles", "roles.permissions"})
	@Query("select account from UserAccountEntity account where account.id = :id")
	Optional<UserAccountEntity> findByIdWithRoles(@Param("id") UUID id);
}