package com.galactique.gestionecole.tuition.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TuitionFeeTypeRepository {
	TuitionFeeType save(TuitionFeeTypeDraft draft);
	List<TuitionFeeType> findAllByEstablishmentId(UUID establishmentId);
	Optional<TuitionFeeType> findByIdAndEstablishmentId(UUID id, UUID establishmentId);
	boolean existsByEstablishmentIdAndCode(UUID establishmentId, String code);
	boolean existsByEstablishmentIdAndName(UUID establishmentId, String name);
	boolean existsByEstablishmentIdAndNameAndIdNot(UUID establishmentId, String name, UUID id);
	TuitionFeeType updateName(UUID id, UUID establishmentId, String name);
	TuitionFeeType disable(UUID id, UUID establishmentId);
}