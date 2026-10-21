package com.galactique.gestionecole.students.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentRepository {
	Student save(UUID establishmentId, String firstName, String lastName);
	Student save(UUID establishmentId, String firstName, String lastName, java.time.LocalDate dateOfBirth);
	Student updateProfile(UUID studentId, UUID establishmentId, String firstName, String lastName, java.time.LocalDate dateOfBirth);

	List<Student> findAllByEstablishmentId(UUID establishmentId);

	List<Student> findActiveByEstablishmentIdAndIds(UUID establishmentId, List<UUID> studentIds);

	Optional<Student> findByIdAndEstablishmentId(UUID studentId, UUID establishmentId);

	Optional<Student> findIncludingArchivedByIdAndEstablishmentId(UUID studentId, UUID establishmentId);

	Student updateProfile(UUID studentId, UUID establishmentId, String firstName, String lastName);

	Student archive(UUID studentId, UUID establishmentId);
}
