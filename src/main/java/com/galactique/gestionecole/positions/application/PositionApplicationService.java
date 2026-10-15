package com.galactique.gestionecole.positions.application;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.establishments.domain.EstablishmentRepository;
import com.galactique.gestionecole.security.domain.SecurityAccountProvisioner;
import com.galactique.gestionecole.security.domain.SecurityDirectoryRepository;
import com.galactique.gestionecole.security.domain.Role;
import com.galactique.gestionecole.employees.application.InvalidEmployeeRolesException;
import com.galactique.gestionecole.employees.application.EmployeeAccessDeniedException;
import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.employees.application.EmployeeConflictException;
import com.galactique.gestionecole.employees.application.EmployeeNotFoundException;
import com.galactique.gestionecole.positions.domain.Position;
import com.galactique.gestionecole.positions.domain.PositionRepository;
import com.galactique.gestionecole.employees.domain.Employee;
import com.galactique.gestionecole.employees.domain.EmployeeRepository;

@Service
public class PositionApplicationService {
	private static final Set<String> RESERVED_ROLE_CODES = Set.of("ADMIN", "PARENT");

	private final PositionRepository positionRepository;
	private final EstablishmentRepository establishmentRepository;
	private final EmployeeRepository employeeRepository;
	private final SecurityAccountProvisioner securityAccountProvisioner;
	private final SecurityDirectoryRepository securityDirectoryRepository;

	public PositionApplicationService(
		PositionRepository positionRepository,
		EstablishmentRepository establishmentRepository,
		EmployeeRepository employeeRepository,
		SecurityAccountProvisioner securityAccountProvisioner,
		SecurityDirectoryRepository securityDirectoryRepository) {
		this.positionRepository = positionRepository;
		this.establishmentRepository = establishmentRepository;
		this.employeeRepository = employeeRepository;
		this.securityAccountProvisioner = securityAccountProvisioner;
		this.securityDirectoryRepository = securityDirectoryRepository;
	}

	@Transactional(readOnly = true)
	public List<Position> findAll(UUID establishmentId, EmployeeActor actor) {
		requireScope(establishmentId, actor);
		return positionRepository.findAllByEstablishmentId(establishmentId).stream()
			.filter(Position::enabled)
			.toList();
	}

	@Transactional
	public Position create(UUID establishmentId, String name, Set<String> requestedRoleCodes, EmployeeActor actor) {
		requireScope(establishmentId, actor);
		if (establishmentRepository.findById(establishmentId).filter(establishment -> establishment.enabled()).isEmpty()) {
			throw new EmployeeNotFoundException();
		}
		Set<String> roleCodes = validateRoleCodes(establishmentId, requestedRoleCodes);
		String normalizedName = name.strip();
		if (positionRepository.existsByEstablishmentIdAndName(establishmentId, normalizedName)) {
			throw new EmployeeConflictException();
		}
		try {
			return positionRepository.save(establishmentId, normalizedName, roleCodes);
		} catch (DataIntegrityViolationException exception) {
			throw new EmployeeConflictException();
		}
	}

	@Transactional
	public Position updateName(UUID establishmentId, UUID positionId, String name, EmployeeActor actor) {
		requireScope(establishmentId, actor);
		Position current = findEnabledPosition(positionId, establishmentId);
		String normalizedName = name.strip();
		if (positionRepository.existsByEstablishmentIdAndNameNot(establishmentId, normalizedName, positionId)) {
			throw new EmployeeConflictException();
		}
		try {
			return positionRepository.updateName(current.id(), establishmentId, normalizedName);
		} catch (DataIntegrityViolationException exception) {
			throw new EmployeeConflictException();
		}
	}

	@Transactional
	public Position updateRoles(
		UUID establishmentId,
		UUID positionId,
		Set<String> requestedRoleCodes,
		EmployeeActor actor) {
		requireScope(establishmentId, actor);
		Position current = findEnabledPosition(positionId, establishmentId);
		Set<String> roleCodes = validateRoleCodes(establishmentId, requestedRoleCodes);
		Position updated = positionRepository.updateRoles(current.id(), establishmentId, roleCodes);
		for (Employee employee : employeeRepository.findAllByPositionId(updated.id())) {
			if (employee.userAccountId() != null && employee.enabled()) {
				securityAccountProvisioner.updateEmployeeRoles(
					employee.userAccountId(), effectiveRoles(employee.directRoleCodes(), updated.roleCodes()));
			}
		}
		return updated;
	}

	@Transactional
	public void disable(UUID establishmentId, UUID positionId, EmployeeActor actor) {
		requireScope(establishmentId, actor);
		Position position = findEnabledPosition(positionId, establishmentId);
		if (!employeeRepository.findAllByPositionId(position.id()).isEmpty()) {
			throw new EmployeeConflictException();
		}
		positionRepository.disable(position.id(), establishmentId);
	}

	private Position findEnabledPosition(UUID positionId, UUID establishmentId) {
		return positionRepository.findByIdAndEstablishmentId(positionId, establishmentId)
			.filter(Position::enabled)
			.orElseThrow(EmployeeNotFoundException::new);
	}

	private void requireScope(UUID establishmentId, EmployeeActor actor) {
		if (actor.globalAdministrator()) {
			return;
		}
		employeeRepository.findByEmail(actor.username())
			.filter(Employee::enabled)
			.filter(Employee::establishmentAdmin)
			.filter(member -> member.userAccountId() != null)
			.filter(member -> member.establishmentId().equals(establishmentId))
			.orElseThrow(EmployeeAccessDeniedException::new);
	}

	private Set<String> validateRoleCodes(UUID establishmentId, Set<String> requestedRoleCodes) {
		Set<String> roleCodes = requestedRoleCodes.stream()
			.map(roleCode -> roleCode.strip().toUpperCase(java.util.Locale.ROOT))
			.collect(java.util.stream.Collectors.toUnmodifiableSet());
		List<Role> establishmentRoles = securityDirectoryRepository.findRolesForEstablishment(establishmentId);
		Set<String> availableRoleCodes = establishmentRoles.stream()
			.filter(Role::enabled)
			.map(Role::code)
			.filter(roleCode -> !RESERVED_ROLE_CODES.contains(roleCode))
			.collect(java.util.stream.Collectors.toSet());
		Set<String> globalAdministratorRoleCodes = establishmentRoles.stream()
			.filter(role -> role.permissions().stream()
				.anyMatch(permission -> "ADMIN_ACCESS".equals(permission.code())))
			.map(Role::code)
			.collect(java.util.stream.Collectors.toSet());
		if (!availableRoleCodes.containsAll(roleCodes)
			|| !java.util.Collections.disjoint(roleCodes, globalAdministratorRoleCodes)) {
			throw new InvalidEmployeeRolesException();
		}
		return roleCodes;
	}

	private Set<String> effectiveRoles(Set<String> directRoles, Set<String> positionRoles) {
		Set<String> effectiveRoles = new LinkedHashSet<>(directRoles);
		effectiveRoles.addAll(positionRoles);
		if (effectiveRoles.isEmpty()) {
			throw new InvalidEmployeeRolesException();
		}
		return Set.copyOf(effectiveRoles);
	}
}