package com.galactique.gestionecole.schoolclasses.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.schoolclasses.domain.SchoolClass;
import com.galactique.gestionecole.schoolclasses.domain.SchoolClassRepository;

@Repository
public class JpaSchoolClassRepositoryAdapter implements SchoolClassRepository {
	private final SchoolClassJpaRepository jpaRepository;

	public JpaSchoolClassRepositoryAdapter(SchoolClassJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	@Transactional
	public SchoolClass save(UUID establishmentId, UUID academicYearId, UUID gradeLevelId, String code, String name) {
		return jpaRepository.save(new SchoolClassEntity(establishmentId, academicYearId, gradeLevelId, code, name))
			.toDomain();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByAcademicYearIdAndCode(UUID academicYearId, String code) {
		return jpaRepository.existsByAcademicYearIdAndCodeIgnoreCase(academicYearId, code);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByAcademicYearIdAndCodeAndIdNot(UUID academicYearId, String code, UUID schoolClassId) {
		return jpaRepository.existsByAcademicYearIdAndCodeIgnoreCaseAndIdNot(academicYearId, code, schoolClassId);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByAcademicYearIdAndGradeLevelIdAndName(
		UUID academicYearId, UUID gradeLevelId, String name) {
		return jpaRepository.existsByAcademicYearIdAndGradeLevelIdAndNameIgnoreCase(academicYearId, gradeLevelId, name);
	}

	@Override
	@Transactional(readOnly = true)
	public List<SchoolClass> findAllByEstablishmentIdAndAcademicYearId(UUID establishmentId, UUID academicYearId) {
		return jpaRepository.findAllByEstablishmentIdAndAcademicYearIdAndEnabledTrueOrderByNameAsc(
			establishmentId, academicYearId).stream().map(SchoolClassEntity::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<SchoolClass> findByIdAndEstablishmentId(UUID schoolClassId, UUID establishmentId) {
		return jpaRepository.findByIdAndEstablishmentIdAndEnabledTrue(schoolClassId, establishmentId)
			.map(SchoolClassEntity::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<SchoolClass> findByIdAndEstablishmentIdAndAcademicYearId(
		UUID schoolClassId, UUID establishmentId, UUID academicYearId) {
		return jpaRepository.findByIdAndEstablishmentIdAndAcademicYearIdAndEnabledTrue(
			schoolClassId, establishmentId, academicYearId).map(SchoolClassEntity::toDomain);
	}

	@Override
	@Transactional
	public SchoolClass update(UUID schoolClassId, UUID establishmentId, UUID academicYearId,
		UUID gradeLevelId, String code, String name) {
		SchoolClassEntity entity = jpaRepository.findByIdAndEstablishmentIdAndAcademicYearIdAndEnabledTrue(
			schoolClassId, establishmentId, academicYearId).orElseThrow();
		entity.update(gradeLevelId, code, name);
		return entity.toDomain();
	}

	@Override
	@Transactional
	public SchoolClass archive(UUID schoolClassId, UUID establishmentId, UUID academicYearId) {
		SchoolClassEntity entity = jpaRepository.findByIdAndEstablishmentIdAndAcademicYearIdAndEnabledTrue(
			schoolClassId, establishmentId, academicYearId).orElseThrow();
		entity.archive();
		return entity.toDomain();
	}
}