package com.galactique.gestionecole.establishments.application;

import java.util.UUID;

public interface EstablishmentEmployeeDeactivation {
	void disableEmployeeForEstablishment(UUID establishmentId);
}