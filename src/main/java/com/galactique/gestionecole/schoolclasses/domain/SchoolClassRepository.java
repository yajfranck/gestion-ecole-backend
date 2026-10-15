package com.galactique.gestionecole.schoolclasses.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchoolClassRepository {
	SchoolClass save(UUID establishmentId, UUID academicYearId, UUID gradeLevelId, String code, String name);

	boolean existsByAcademicYearIdAndCode(UUID academicYearId, String code);

	boolean existsByAcademicYearIdAndCodeAndIdNot(UUID academicYearId, String code, UUID schoolClassId);

	boolean existsByAcademicYearIdAndGradeLevelIdAndName(
		UUID academicYearId, UUID gradeLevelId, String name);

	List<SchoolClass> findAllByEstablishmentIdAndAcademicYearId(UUID establishmentId, UUID academicYearId);

	Optional<SchoolClass> findByIdAndEstablishmentId(UUID schoolClassId, UUID establishmentId);

	Optional<SchoolClass> findByIdAndEstablishmentIdAndAcademicYearId(
		UUID schoolClassId, UUID establishmentId, UUID academicYearId);

	SchoolClass update(UUID schoolClassId, UUID establishmentId, UUID academicYearId,
		UUID gradeLevelId, String code, String name);

	SchoolClass archive(UUID schoolClassId, UUID establishmentId, UUID academicYearId);
}