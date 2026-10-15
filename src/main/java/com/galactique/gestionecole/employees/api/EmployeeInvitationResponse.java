package com.galactique.gestionecole.employees.api;

import java.util.UUID;

public record EmployeeInvitationResponse(UUID employeeId, String status) {
}