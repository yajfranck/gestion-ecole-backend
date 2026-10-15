package com.galactique.gestionecole.employees.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface EmployeeInvitationJpaRepository extends JpaRepository<EmployeeInvitationEntity, UUID> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select invitation from EmployeeInvitationEntity invitation where invitation.tokenHash = :tokenHash")
	Optional<EmployeeInvitationEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}