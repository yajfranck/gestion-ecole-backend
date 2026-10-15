package com.galactique.gestionecole.positions.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PositionRepository {
	Position save(UUID establishmentId, String name, Set<String> roleCodes);

	boolean existsByEstablishmentIdAndName(UUID establishmentId, String name);

	boolean existsByEstablishmentIdAndNameNot(UUID establishmentId, String name, UUID id);

	Optional<Position> findByIdAndEstablishmentId(UUID id, UUID establishmentId);

	Optional<Position> findById(UUID id);

	List<Position> findAllByEstablishmentId(UUID establishmentId);

	Position updateName(UUID id, UUID establishmentId, String name);

	Position updateRoles(UUID id, UUID establishmentId, Set<String> roleCodes);

	Position disable(UUID id, UUID establishmentId);
}