package com.galactique.gestionecole.schoolclasses.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.schoolclasses.domain.AcademicYear;
import com.galactique.gestionecole.schoolclasses.domain.AcademicYearRepository;

@Repository
public class JpaAcademicYearRepositoryAdapter implements AcademicYearRepository {
	private final AcademicYearJpaRepository jpaRepository;

	public JpaAcademicYearRepositoryAdapter(AcademicYearJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	@Transactional
	public AcademicYear save(UUID establishmentId, String name, LocalDate startsOn, LocalDate endsOn) {
		return jpaRepository.save(new AcademicYearEntity(establishmentId, name.strip(), startsOn, endsOn)).toDomain();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByEstablishmentIdAndName(UUID establishmentId, String name) {
		return jpaRepository.existsByEstablishmentIdAndNameIgnoreCase(establishmentId, name);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByEstablishmentIdAndNameNot(UUID establishmentId, String name, UUID academicYearId) {
		return jpaRepository.existsByEstablishmentIdAndNameIgnoreCaseAndIdNot(establishmentId, name, academicYearId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<AcademicYear> findAllByEstablishmentId(UUID establishmentId) {
		return jpaRepository.findAllByEstablishmentIdAndEnabledTrueOrderByStartsOnDesc(establishmentId).stream()
			.map(AcademicYearEntity::toDomain)
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<AcademicYear> findByIdAndEstablishmentId(UUID academicYearId, UUID establishmentId) {
		return jpaRepository.findByIdAndEstablishmentIdAndEnabledTrue(academicYearId, establishmentId)
			.map(AcademicYearEntity::toDomain);
	}

	@Override
	@Transactional
	public AcademicYear update(UUID academicYearId, UUID establishmentId, String name,
		LocalDate startsOn, LocalDate endsOn) {
		AcademicYearEntity entity = jpaRepository
			.findByIdAndEstablishmentIdAndEnabledTrue(academicYearId, establishmentId).orElseThrow();
		entity.update(name.strip(), startsOn, endsOn);
		return entity.toDomain();
	}

	@Override
	@Transactional
	public Optional<AcademicYear> findForUpdate(UUID academicYearId, UUID establishmentId) {
		return jpaRepository.findForUpdate(academicYearId, establishmentId).map(AcademicYearEntity::toDomain);
	}

	@Override
	@Transactional
	public AcademicYear disable(UUID academicYearId, UUID establishmentId) {
		AcademicYearEntity entity = jpaRepository
			.findByIdAndEstablishmentIdAndEnabledTrue(academicYearId, establishmentId).orElseThrow();
		entity.disable();
		return entity.toDomain();
	}
}
