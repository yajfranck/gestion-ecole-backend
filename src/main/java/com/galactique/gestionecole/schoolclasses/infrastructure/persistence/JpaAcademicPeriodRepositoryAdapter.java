package com.galactique.gestionecole.schoolclasses.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.schoolclasses.domain.AcademicPeriod;
import com.galactique.gestionecole.schoolclasses.domain.AcademicPeriodRepository;

@Repository
public class JpaAcademicPeriodRepositoryAdapter implements AcademicPeriodRepository {
	private final AcademicPeriodJpaRepository jpaRepository;

	public JpaAcademicPeriodRepositoryAdapter(AcademicPeriodJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	@Transactional
	public AcademicPeriod save(UUID academicYearId, String name, LocalDate startsOn, LocalDate endsOn) {
		return jpaRepository.save(new AcademicPeriodEntity(academicYearId, name.strip(), startsOn, endsOn)).toDomain();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByAcademicYearIdAndName(UUID academicYearId, String name) {
		return jpaRepository.existsByAcademicYearIdAndNameIgnoreCase(academicYearId, name);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByAcademicYearIdAndNameNot(UUID academicYearId, String name, UUID periodId) {
		return jpaRepository.existsByAcademicYearIdAndNameIgnoreCaseAndIdNot(academicYearId, name, periodId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<AcademicPeriod> findAllByAcademicYearId(UUID academicYearId) {
		return jpaRepository.findAllByAcademicYearIdOrderByStartsOnAsc(academicYearId).stream()
			.map(AcademicPeriodEntity::toDomain)
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<AcademicPeriod> findByIdAndAcademicYearId(UUID periodId, UUID academicYearId) {
		return jpaRepository.findByIdAndAcademicYearIdAndEnabledTrue(periodId, academicYearId)
			.map(AcademicPeriodEntity::toDomain);
	}

	@Override
	@Transactional
	public AcademicPeriod update(UUID periodId, UUID academicYearId, String name,
		LocalDate startsOn, LocalDate endsOn) {
		AcademicPeriodEntity entity = jpaRepository.findByIdAndAcademicYearIdAndEnabledTrue(periodId, academicYearId)
			.orElseThrow();
		entity.update(name.strip(), startsOn, endsOn);
		return entity.toDomain();
	}

	@Override
	@Transactional
	public AcademicPeriod disable(UUID periodId, UUID academicYearId) {
		AcademicPeriodEntity entity = jpaRepository.findByIdAndAcademicYearIdAndEnabledTrue(periodId, academicYearId)
			.orElseThrow();
		entity.disable();
		return entity.toDomain();
	}
}