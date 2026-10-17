package com.galactique.gestionecole.guardians.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.galactique.gestionecole.students.domain.Student;

public interface GuardianRepository {
	Optional<Guardian> findByEmail(String email);
	Optional<Guardian> findByUserAccountId(UUID userAccountId);
	Optional<Guardian> findByIdForUpdate(UUID guardianId);
	Guardian save(String firstName, String lastName, String email);
	boolean existsLink(UUID guardianId, UUID studentId);
	Optional<Student> findLinkedChild(UUID guardianId, UUID studentId);
	void link(UUID guardianId, UUID studentId);
	void unlink(UUID guardianId, UUID studentId);
	List<Guardian> findAllByStudentId(UUID studentId);
	List<Student> findChildren(UUID guardianId);
	Guardian attachUserAccount(UUID guardianId, UUID userAccountId);
}
