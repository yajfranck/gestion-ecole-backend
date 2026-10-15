package com.galactique.gestionecole.employees.application;

import java.util.UUID;

public record UpdateEmployeeCommand(String employeeNumber, String firstName, String lastName, String email, UUID positionId) {
}