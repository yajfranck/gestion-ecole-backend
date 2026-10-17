package com.galactique.gestionecole.schoolclasses.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcademicYearRepository {
	AcademicYear save(UUID establishmentId, String name, LocalDate startsOn, LocalDate endsOn);

	boolean existsByEstablishmentIdAndName(UUID establishmentId, String name);

	boolean existsByEstablishmentIdAndNameNot(UUID establishmentId, String name, UUID academicYearId);

	List<AcademicYear> findAllByEstablishmentId(UUID establishmentId);

	Optional<AcademicYear> findByIdAndEstablishmentId(UUID academicYearId, UUID establishmentId);

	Optional<AcademicYear> findForUpdate(UUID academicYearId, UUID establishmentId);

	AcademicYear update(UUID academicYearId, UUID establishmentId, String name, LocalDate startsOn, LocalDate endsOn);

	AcademicYear disable(UUID academicYearId, UUID establishmentId);
}
