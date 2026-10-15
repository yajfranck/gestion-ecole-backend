package com.galactique.gestionecole.establishments.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EstablishmentRepository {
	Establishment save(String code, String name);

	Optional<Establishment> findById(UUID id);

	List<Establishment> findAll();

	Establishment disable(UUID id);
}