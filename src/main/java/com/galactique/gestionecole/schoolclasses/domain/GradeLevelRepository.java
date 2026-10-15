package com.galactique.gestionecole.schoolclasses.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GradeLevelRepository {
	GradeLevel save(UUID establishmentId, String code, String name, int displayOrder);

	boolean existsByEstablishmentIdAndCode(UUID establishmentId, String code);

	boolean existsByEstablishmentIdAndCodeAndIdNot(UUID establishmentId, String code, UUID gradeLevelId);

	boolean existsByEstablishmentIdAndName(UUID establishmentId, String name);

	boolean existsByEstablishmentIdAndNameAndIdNot(UUID establishmentId, String name, UUID gradeLevelId);

	boolean hasActiveClasses(UUID establishmentId, UUID gradeLevelId);

	List<GradeLevel> findAllByEstablishmentId(UUID establishmentId);

	Optional<GradeLevel> findByIdAndEstablishmentId(UUID gradeLevelId, UUID establishmentId);

	GradeLevel update(UUID gradeLevelId, UUID establishmentId, String code, String name, int displayOrder);

	GradeLevel archive(UUID gradeLevelId, UUID establishmentId);
}