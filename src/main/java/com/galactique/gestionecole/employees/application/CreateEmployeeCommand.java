package com.galactique.gestionecole.employees.application;

import java.util.UUID;
import java.util.Set;

public record CreateEmployeeCommand(
	String employeeNumber,
	String firstName,
	String lastName,
	String email,
	UUID positionId,
	Set<String> directRoleCodes) {
}