package com.galactique.gestionecole.schoolclasses.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.schoolclasses.domain.GradeLevel;
import com.galactique.gestionecole.schoolclasses.domain.GradeLevelRepository;

@Repository
public class JpaGradeLevelRepositoryAdapter implements GradeLevelRepository {
	private final GradeLevelJpaRepository jpaRepository;
	private final SchoolClassJpaRepository schoolClassJpaRepository;

	public JpaGradeLevelRepositoryAdapter(GradeLevelJpaRepository jpaRepository,
		SchoolClassJpaRepository schoolClassJpaRepository) {
		this.jpaRepository = jpaRepository;
		this.schoolClassJpaRepository = schoolClassJpaRepository;
	}

	@Override
	@Transactional
	public GradeLevel save(UUID establishmentId, String code, String name, int displayOrder) {
		return jpaRepository.save(new GradeLevelEntity(establishmentId, code, name, displayOrder)).toDomain();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByEstablishmentIdAndCode(UUID establishmentId, String code) {
		return jpaRepository.existsByEstablishmentIdAndCodeIgnoreCase(establishmentId, code);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByEstablishmentIdAndCodeAndIdNot(UUID establishmentId, String code, UUID gradeLevelId) {
		return jpaRepository.existsByEstablishmentIdAndCodeIgnoreCaseAndIdNot(establishmentId, code, gradeLevelId);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByEstablishmentIdAndName(UUID establishmentId, String name) {
		return jpaRepository.existsByEstablishmentIdAndNameIgnoreCase(establishmentId, name);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByEstablishmentIdAndNameAndIdNot(UUID establishmentId, String name, UUID gradeLevelId) {
		return jpaRepository.existsByEstablishmentIdAndNameIgnoreCaseAndIdNot(establishmentId, name, gradeLevelId);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean hasActiveClasses(UUID establishmentId, UUID gradeLevelId) {
		return schoolClassJpaRepository.existsByEstablishmentIdAndGradeLevelIdAndEnabledTrue(establishmentId, gradeLevelId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<GradeLevel> findAllByEstablishmentId(UUID establishmentId) {
		return jpaRepository.findAllByEstablishmentIdAndEnabledTrueOrderByDisplayOrderAscNameAsc(establishmentId)
			.stream().map(GradeLevelEntity::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<GradeLevel> findByIdAndEstablishmentId(UUID gradeLevelId, UUID establishmentId) {
		return jpaRepository.findByIdAndEstablishmentIdAndEnabledTrue(gradeLevelId, establishmentId)
			.map(GradeLevelEntity::toDomain);
	}

	@Override
	@Transactional
	public GradeLevel update(UUID gradeLevelId, UUID establishmentId, String code, String name, int displayOrder) {
		GradeLevelEntity entity = jpaRepository.findByIdAndEstablishmentIdAndEnabledTrue(gradeLevelId, establishmentId)
			.orElseThrow();
		entity.update(code, name, displayOrder);
		return entity.toDomain();
	}

	@Override
	@Transactional
	public GradeLevel archive(UUID gradeLevelId, UUID establishmentId) {
		GradeLevelEntity entity = jpaRepository.findByIdAndEstablishmentIdAndEnabledTrue(gradeLevelId, establishmentId)
			.orElseThrow();
		entity.archive();
		return entity.toDomain();
	}
}