package com.galactique.gestionecole.schoolclasses.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubjectRepository {
	Subject save(UUID establishmentId, String code, String name);

	boolean existsByEstablishmentIdAndCode(UUID establishmentId, String code);

	boolean existsByEstablishmentIdAndCodeAndIdNot(UUID establishmentId, String code, UUID subjectId);

	boolean hasActiveClassAssignments(UUID establishmentId, UUID subjectId);

	List<Subject> findAllByEstablishmentId(UUID establishmentId);

	Optional<Subject> findByIdAndEstablishmentId(UUID subjectId, UUID establishmentId);

	Subject update(UUID subjectId, UUID establishmentId, String code, String name);

	Subject archive(UUID subjectId, UUID establishmentId);
}