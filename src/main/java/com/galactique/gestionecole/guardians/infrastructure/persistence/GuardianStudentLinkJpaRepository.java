package com.galactique.gestionecole.guardians.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import com.galactique.gestionecole.students.infrastructure.persistence.StudentEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuardianStudentLinkJpaRepository extends JpaRepository<GuardianStudentLinkEntity, UUID> {
	boolean existsByGuardianIdAndStudentId(UUID guardianId, UUID studentId);

	@Query("select student from StudentEntity student, GuardianStudentLinkEntity link, "
		+ "GuardianEntity guardian, EstablishmentEntity establishment "
		+ "where link.guardianId = :guardianId and link.studentId = :studentId "
		+ "and guardian.id = link.guardianId and guardian.enabled = true "
		+ "and guardian.userAccountId is not null and student.id = link.studentId and student.enabled = true "
		+ "and establishment.id = student.establishmentId and establishment.enabled = true")
	java.util.Optional<StudentEntity> findLinkedActiveChild(
		@Param("guardianId") UUID guardianId, @Param("studentId") UUID studentId);

	@Modifying
	@Query("delete from GuardianStudentLinkEntity link where link.guardianId = :guardianId and link.studentId = :studentId")
	void deleteByGuardianIdAndStudentId(@Param("guardianId") UUID guardianId, @Param("studentId") UUID studentId);

	@Query("""
		select guardian from GuardianEntity guardian, GuardianStudentLinkEntity link
		where link.studentId = :studentId and link.guardianId = guardian.id and guardian.enabled = true
		order by guardian.lastName, guardian.firstName, guardian.email
		""")
	List<GuardianEntity> findGuardiansByStudentId(@Param("studentId") UUID studentId);

	@Query("""
		select distinct student from StudentEntity student, GuardianStudentLinkEntity link,
			EstablishmentEntity establishment
		where link.guardianId = :guardianId and link.studentId = student.id
			and student.enabled = true and establishment.id = student.establishmentId and establishment.enabled = true
		order by student.lastName, student.firstName, student.studentNumber
		""")
	List<com.galactique.gestionecole.students.infrastructure.persistence.StudentEntity> findChildren(
		@Param("guardianId") UUID guardianId);
}