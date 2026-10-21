package com.galactique.gestionecole.employees.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface EmployeeJpaRepository extends JpaRepository<EmployeeEntity, UUID> {
	@Override
	@EntityGraph(attributePaths = {"position", "position.roleCodes", "directRoleCodes"})
	List<EmployeeEntity> findAll();

	@Override
	@EntityGraph(attributePaths = {"position", "position.roleCodes", "directRoleCodes"})
	Optional<EmployeeEntity> findById(UUID id);

	@EntityGraph(attributePaths = {"position", "position.roleCodes", "directRoleCodes"})
	Optional<EmployeeEntity> findByEmailIgnoreCase(String email);

	@EntityGraph(attributePaths = {"position", "position.roleCodes", "directRoleCodes"})
	Optional<EmployeeEntity> findByEmailIgnoreCaseAndEstablishmentId(String email, UUID establishmentId);

	@EntityGraph(attributePaths = {"position", "position.roleCodes", "directRoleCodes"})
	Optional<EmployeeEntity> findByUserAccountIdAndEstablishmentId(UUID userAccountId, UUID establishmentId);

	@EntityGraph(attributePaths = {"position", "position.roleCodes", "directRoleCodes"})
	List<EmployeeEntity> findAllByUserAccountId(UUID userAccountId);

	boolean existsByEmployeeNumber(String employeeNumber);
	boolean existsByEmployeeNumberAndIdNot(String employeeNumber, UUID id);

	boolean existsByEmailIgnoreCase(String email);
	boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

	@EntityGraph(attributePaths = {"position", "position.roleCodes", "directRoleCodes"})
	List<EmployeeEntity> findAllByEstablishmentId(UUID establishmentId);

	@EntityGraph(attributePaths = {"position", "position.roleCodes", "directRoleCodes"})
	List<EmployeeEntity> findAllByPosition_Id(UUID positionId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = {"position", "position.roleCodes", "directRoleCodes"})
	@Query("select employee from EmployeeEntity employee where employee.id = :id")
	Optional<EmployeeEntity> findByIdForUpdate(@Param("id") UUID id);
}
