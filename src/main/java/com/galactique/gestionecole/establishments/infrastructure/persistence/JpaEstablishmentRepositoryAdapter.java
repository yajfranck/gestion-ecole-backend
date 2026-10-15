package com.galactique.gestionecole.establishments.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.establishments.domain.Establishment;
import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;

@Repository
public class JpaEstablishmentRepositoryAdapter implements EstablishmentRepository {
	private final EstablishmentJpaRepository jpaRepository;

	public JpaEstablishmentRepositoryAdapter(EstablishmentJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	@Transactional
	public Establishment save(String code, String name) {
		return toDomain(jpaRepository.save(new EstablishmentEntity(code, name)));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Establishment> findById(UUID id) {
		return jpaRepository.findById(id).map(this::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Establishment> findAll() {
		return jpaRepository.findAll().stream().map(this::toDomain).toList();
	}

	@Override
	@Transactional
	public Establishment disable(UUID id) {
		EstablishmentEntity entity = jpaRepository.findById(id).orElseThrow();
		entity.disable();
		return toDomain(entity);
	}

	private Establishment toDomain(EstablishmentEntity entity) {
		return new Establishment(entity.getId(), entity.getCode(), entity.getName(), entity.isEnabled(),
			entity.getCreatedAt(), entity.getUpdatedAt());
	}
}