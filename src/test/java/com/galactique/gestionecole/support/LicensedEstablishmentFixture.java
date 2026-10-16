package com.galactique.gestionecole.support;

import java.util.UUID;

import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.licenses.application.LicenseApplicationService;

public final class LicensedEstablishmentFixture {
	private static final UUID LEGACY_OFFER_ID = UUID.fromString("c51d6db7-309f-4e85-b884-000000000001");

	private LicensedEstablishmentFixture() {
	}

	public static Establishment create(EstablishmentRepository establishmentRepository,
		LicenseApplicationService licenseApplicationService, String code, String name) {
		Establishment establishment = establishmentRepository.save(code, name);
		licenseApplicationService.assign(establishment.id(), LEGACY_OFFER_ID, null, null, "TEST_FIXTURE");
		return establishment;
	}
}
