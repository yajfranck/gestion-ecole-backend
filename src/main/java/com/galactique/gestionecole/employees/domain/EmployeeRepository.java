package com.galactique.gestionecole.employees.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface EmployeeRepository {
	Employee save(String employeeNumber, String firstName, String lastName, String email,
		UUID positionId, Set<String> directRoleCodes);

	boolean existsByEmployeeNumber(String employeeNumber);

	boolean existsByEmployeeNumberAndIdNot(String employeeNumber, UUID employeeId);

	boolean existsByEmail(String email);

	boolean existsByEmailAndIdNot(String email, UUID employeeId);

	Optional<Employee> findById(UUID id);

	Optional<Employee> findByEmail(String email);

	Optional<Employee> findByIdForUpdate(UUID id);

	List<Employee> findAll();

	List<Employee> findAllByEstablishmentId(UUID establishmentId);

	List<Employee> findAllByPositionId(UUID positionId);

	Employee attachUserAccount(UUID employeeId, UUID userAccountId);

	Employee updateProfile(UUID employeeId, String employeeNumber, String firstName, String lastName,
		String email, UUID positionId);

	Employee updateRoles(UUID employeeId, Set<String> directRoleCodes);

	Employee disable(UUID employeeId);
}