package com.galactique.gestionecole.schoolclasses.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchoolClassRepository {
	SchoolClass save(UUID establishmentId, UUID academicYearId, String code, String name, String series);

	boolean existsByAcademicYearIdAndCode(UUID academicYearId, String code);

	boolean existsByAcademicYearIdAndCodeAndIdNot(UUID academicYearId, String code, UUID schoolClassId);

	List<SchoolClass> findAllByEstablishmentIdAndAcademicYearId(UUID establishmentId, UUID academicYearId);

	Optional<SchoolClass> findByIdAndEstablishmentId(UUID schoolClassId, UUID establishmentId);

	Optional<SchoolClass> findByIdAndEstablishmentIdAndAcademicYearId(
		UUID schoolClassId, UUID establishmentId, UUID academicYearId);

	Optional<SchoolClass> findIncludingArchivedByIdAndEstablishmentIdAndAcademicYearId(
		UUID schoolClassId, UUID establishmentId, UUID academicYearId);

	SchoolClass update(UUID schoolClassId, UUID establishmentId, UUID academicYearId, String code, String name,
		String series);

	SchoolClass archive(UUID schoolClassId, UUID establishmentId, UUID academicYearId);

	Optional<SchoolClass> findForUpdate(UUID schoolClassId, UUID establishmentId, UUID academicYearId);
}
