package com.galactique.gestionecole.schoolclasses.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcademicPeriodRepository {
	AcademicPeriod save(UUID academicYearId, String name, LocalDate startsOn, LocalDate endsOn);

	boolean existsByAcademicYearIdAndName(UUID academicYearId, String name);

	boolean existsByAcademicYearIdAndNameNot(UUID academicYearId, String name, UUID periodId);

	List<AcademicPeriod> findAllByAcademicYearId(UUID academicYearId);

	Optional<AcademicPeriod> findByIdAndAcademicYearId(UUID periodId, UUID academicYearId);

	AcademicPeriod update(UUID periodId, UUID academicYearId, String name, LocalDate startsOn, LocalDate endsOn);

	AcademicPeriod disable(UUID periodId, UUID academicYearId);
}