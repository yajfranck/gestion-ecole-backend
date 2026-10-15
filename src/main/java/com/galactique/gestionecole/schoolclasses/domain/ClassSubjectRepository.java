package com.galactique.gestionecole.schoolclasses.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassSubjectRepository {
	Optional<SchoolClassSubject> findByEstablishmentIdAndClassIdAndSubjectId(
		UUID establishmentId, UUID schoolClassId, UUID subjectId);

	SchoolClassSubject save(UUID establishmentId, UUID schoolClassId, UUID subjectId);

	List<SchoolClassSubject> findAllByEstablishmentIdAndClassId(UUID establishmentId, UUID schoolClassId);

	SchoolClassSubject enable(UUID assignmentId, UUID establishmentId);

	SchoolClassSubject disable(UUID assignmentId, UUID establishmentId);
}