package com.galactique.gestionecole.positions.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.positions.domain.Position;
import com.galactique.gestionecole.positions.domain.PositionRepository;

@Repository
public class JpaPositionRepositoryAdapter implements PositionRepository {
	private final PositionJpaRepository jpaRepository;

	public JpaPositionRepositoryAdapter(PositionJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	@Transactional
	public Position save(UUID establishmentId, String name, Set<String> roleCodes) {
		return toDomain(jpaRepository.save(new PositionEntity(establishmentId, name, roleCodes)));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Position> findByIdAndEstablishmentId(UUID id, UUID establishmentId) {
		return jpaRepository.findByIdAndEstablishmentId(id, establishmentId).map(this::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Position> findById(UUID id) {
		return jpaRepository.findById(id).map(this::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Position> findAllByEstablishmentId(UUID establishmentId) {
		return jpaRepository.findAllByEstablishmentIdOrderByName(establishmentId).stream()
			.map(this::toDomain)
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByEstablishmentIdAndName(UUID establishmentId, String name) {
		return jpaRepository.existsByEstablishmentIdAndNameIgnoreCase(establishmentId, name);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByEstablishmentIdAndNameNot(UUID establishmentId, String name, UUID id) {
		return jpaRepository.existsByEstablishmentIdAndNameIgnoreCaseAndIdNot(establishmentId, name, id);
	}

	@Override
	@Transactional
	public Position updateName(UUID id, UUID establishmentId, String name) {
		PositionEntity entity = jpaRepository.findWithRolesByIdAndEstablishmentId(id, establishmentId)
			.orElseThrow();
		entity.updateName(name);
		return toDomain(entity);
	}

	@Override
	@Transactional
	public Position updateRoles(UUID id, UUID establishmentId, Set<String> roleCodes) {
		PositionEntity entity = jpaRepository.findWithRolesByIdAndEstablishmentId(id, establishmentId)
			.orElseThrow();
		entity.updateRoleCodes(roleCodes);
		return toDomain(entity);
	}

	@Override
	@Transactional
	public Position disable(UUID id, UUID establishmentId) {
		PositionEntity entity = jpaRepository.findWithRolesByIdAndEstablishmentId(id, establishmentId)
			.orElseThrow();
		entity.disable();
		return toDomain(entity);
	}

	private Position toDomain(PositionEntity entity) {
		return new Position(entity.getId(), entity.getEstablishmentId(), entity.getName(), entity.isEnabled(),
			entity.getRoleCodes(), entity.getCreatedAt(), entity.getUpdatedAt());
	}
}