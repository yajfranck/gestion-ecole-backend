package com.galactique.gestionecole.employees.api.dto;

import java.util.UUID;

public record EmployeeInvitationResponse(UUID employeeId, String status) {
}
