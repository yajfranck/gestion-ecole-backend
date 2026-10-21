package com.galactique.gestionecole.employees.application;

import java.util.UUID;

public record EmployeeActor(String username, UUID scopedEstablishmentId) {
	public EmployeeActor(String username) { this(username, null); }
	public static EmployeeActor scopedStaff(String username, UUID establishmentId) {
		return new EmployeeActor(username, establishmentId);
	}
}
