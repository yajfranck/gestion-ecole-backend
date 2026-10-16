package com.galactique.gestionecole.employees.application;

import java.util.UUID;

public record EmployeeActor(String username, boolean globalAdministrator, boolean establishmentAdministrator,
	UUID scopedEstablishmentId) {
	public EmployeeActor(String username, boolean globalAdministrator, boolean establishmentAdministrator) {
		this(username, globalAdministrator, establishmentAdministrator, null);
	}

	public static EmployeeActor scopedStaff(String username, UUID establishmentId) {
		return new EmployeeActor(username, false, false, establishmentId);
	}
}