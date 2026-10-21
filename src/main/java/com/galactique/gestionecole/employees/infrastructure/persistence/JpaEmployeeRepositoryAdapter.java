package com.galactique.gestionecole.employees.infrastructure.persistence;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.employees.domain.Employee;
import com.galactique.gestionecole.employees.domain.EmployeeRepository;
import com.galactique.gestionecole.positions.infrastructure.persistence.PositionEntity;
import com.galactique.gestionecole.positions.infrastructure.persistence.PositionJpaRepository;

@Repository
public class JpaEmployeeRepositoryAdapter implements EmployeeRepository {
	private final EmployeeJpaRepository jpaRepository;
	private final PositionJpaRepository positionJpaRepository;

	public JpaEmployeeRepositoryAdapter(EmployeeJpaRepository jpaRepository,
		PositionJpaRepository positionJpaRepository) {
		this.jpaRepository = jpaRepository;
		this.positionJpaRepository = positionJpaRepository;
	}

	@Override
	@Transactional
	public Employee save(String employeeNumber, String firstName, String lastName, String email,
		UUID positionId, Set<String> directRoleCodes) {
		PositionEntity position = positionJpaRepository.findById(positionId).orElseThrow();
		EmployeeEntity entity = new EmployeeEntity(employeeNumber, firstName, lastName, email,
			position, directRoleCodes, position.getEstablishmentId());
		return toDomain(jpaRepository.save(entity));
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByEmployeeNumber(String employeeNumber) {
		return jpaRepository.existsByEmployeeNumber(employeeNumber);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByEmployeeNumberAndIdNot(String employeeNumber, UUID employeeId) {
		return jpaRepository.existsByEmployeeNumberAndIdNot(employeeNumber, employeeId);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByEmail(String email) {
		return jpaRepository.existsByEmailIgnoreCase(email);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByEmailAndIdNot(String email, UUID employeeId) {
		return jpaRepository.existsByEmailIgnoreCaseAndIdNot(email, employeeId);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Employee> findById(UUID id) {
		return jpaRepository.findById(id).map(this::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Employee> findByEmail(String email) {
		return jpaRepository.findByEmailIgnoreCase(email).map(this::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Employee> findByEmailAndEstablishmentId(String email, UUID establishmentId) {
		return jpaRepository.findByEmailIgnoreCaseAndEstablishmentId(email, establishmentId).map(this::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Employee> findByUserAccountIdAndEstablishmentId(UUID userAccountId, UUID establishmentId) {
		return jpaRepository.findByUserAccountIdAndEstablishmentId(userAccountId, establishmentId).map(this::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Employee> findAllByUserAccountId(UUID userAccountId) {
		return jpaRepository.findAllByUserAccountId(userAccountId).stream().map(this::toDomain).toList();
	}

	@Override
	@Transactional
	public Optional<Employee> findByIdForUpdate(UUID id) {
		return jpaRepository.findByIdForUpdate(id).map(this::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Employee> findAll() {
		return jpaRepository.findAll().stream().map(this::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Employee> findAllByEstablishmentId(UUID establishmentId) {
		return jpaRepository.findAllByEstablishmentId(establishmentId).stream().map(this::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Employee> findAllByPositionId(UUID positionId) {
		return jpaRepository.findAllByPosition_Id(positionId).stream().map(this::toDomain).toList();
	}

	@Override
	@Transactional
	public Employee attachUserAccount(UUID employeeId, UUID userAccountId) {
		EmployeeEntity entity = jpaRepository.findByIdForUpdate(employeeId).orElseThrow();
		entity.attachUserAccount(userAccountId);
		return toDomain(entity);
	}

	@Override
	@Transactional
	public Employee updateProfile(UUID employeeId, String employeeNumber, String firstName,
		String lastName, String email, UUID positionId) {
		EmployeeEntity entity = jpaRepository.findByIdForUpdate(employeeId).orElseThrow();
		PositionEntity position = positionJpaRepository.findById(positionId).orElseThrow();
		entity.updateProfile(employeeNumber, firstName, lastName, email, position);
		return toDomain(entity);
	}

	@Override
	@Transactional
	public Employee updateRoles(UUID employeeId, Set<String> directRoleCodes) {
		EmployeeEntity entity = jpaRepository.findByIdForUpdate(employeeId).orElseThrow();
		entity.updateRoles(directRoleCodes);
		return toDomain(entity);
	}

	@Override
	@Transactional
	public Employee disable(UUID employeeId) {
		EmployeeEntity entity = jpaRepository.findByIdForUpdate(employeeId).orElseThrow();
		entity.disable();
		return toDomain(entity);
	}

	private Employee toDomain(EmployeeEntity entity) {
		Set<String> effectiveRoleCodes = new LinkedHashSet<>(entity.getDirectRoleCodes());
		if (entity.getPosition().isEnabled()) { effectiveRoleCodes.addAll(entity.getPosition().getRoleCodes()); }
		return new Employee(
			entity.getId(),
			entity.getEmployeeNumber(),
			entity.getFirstName(),
			entity.getLastName(),
			entity.getEmail(),
			entity.getPosition().getId(),
			entity.getPosition().getName(),
			entity.isEnabled(),
			entity.getDirectRoleCodes(),
			Set.copyOf(effectiveRoleCodes),
			entity.getEstablishmentId(),
			entity.getUserAccountId(),
			entity.getCreatedAt(),
			entity.getUpdatedAt());
	}
}
