package com.galactique.gestionecole.employees.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.galactique.gestionecole.employees.domain.Employee;

public record EmployeeResponse(
	UUID id,
	String employeeNumber,
	String firstName,
	String lastName,
	String email,
	UUID positionId,
	String positionName,
	boolean enabled,
	boolean establishmentAdmin,
	List<String> directRoleCodes,
	List<String> roles,
	UUID establishmentId,
	boolean loginEnabled,
	Instant createdAt,
	Instant updatedAt) {
	public static EmployeeResponse from(Employee employee) {
		return new EmployeeResponse(
			employee.id(),
			employee.employeeNumber(),
			employee.firstName(),
			employee.lastName(),
			employee.email(),
			employee.positionId(),
			employee.positionName(),
			employee.enabled(),
			employee.establishmentAdmin(),
			employee.directRoleCodes().stream().sorted().toList(),
			employee.roleCodes().stream().sorted().toList(),
			employee.establishmentId(),
			employee.userAccountId() != null,
			employee.createdAt(),
			employee.updatedAt());
	}
}