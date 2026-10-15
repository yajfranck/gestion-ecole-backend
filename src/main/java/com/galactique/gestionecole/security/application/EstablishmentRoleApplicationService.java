package com.galactique.gestionecole.security.application;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.employees.application.EmployeeActor;
import com.galactique.gestionecole.employees.domain.Employee;
import com.galactique.gestionecole.employees.domain.EmployeeRepository;
import com.galactique.gestionecole.security.domain.Permission;
import com.galactique.gestionecole.security.domain.Role;
import com.galactique.gestionecole.security.domain.SecurityDirectoryRepository;

@Service
public class EstablishmentRoleApplicationService {
	private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]{0,63}");
	private static final Set<String> PLATFORM_PERMISSIONS = Set.of(
		"ADMIN_ACCESS", "can_establishment_onboard", "can_establishment_read");

	private final SecurityDirectoryRepository securityDirectoryRepository;
	private final EmployeeRepository employeeRepository;

	public EstablishmentRoleApplicationService(SecurityDirectoryRepository securityDirectoryRepository,
		EmployeeRepository employeeRepository) {
		this.securityDirectoryRepository = securityDirectoryRepository;
		this.employeeRepository = employeeRepository;
	}

	@Transactional(readOnly = true)
	public List<Role> findRoles(UUID establishmentId, EmployeeActor actor) {
		requireScope(establishmentId, actor);
		return securityDirectoryRepository.findRolesForEstablishment(establishmentId);
	}

	@Transactional(readOnly = true)
	public List<Permission> findPermissions(UUID establishmentId, EmployeeActor actor) {
		requireScope(establishmentId, actor);
		return securityDirectoryRepository.findAllPermissions().stream()
			.filter(permission -> !PLATFORM_PERMISSIONS.contains(permission.code()))
			.sorted(java.util.Comparator.comparing(Permission::code))
			.toList();
	}

	@Transactional
	public Role createRole(UUID establishmentId, CreateRoleCommand command, EmployeeActor actor) {
		requireScope(establishmentId, actor);
		String code = normalizeCode(command.code());
		if (securityDirectoryRepository.findRoleByCode(code).isPresent()) {
			throw new RoleAlreadyExistsException();
		}
		Role role = buildRole(code, command.name(), command.description(), command.permissionCodes(), establishmentId);
		try {
			return securityDirectoryRepository.createRole(role);
		} catch (DataIntegrityViolationException exception) {
			throw new RoleAlreadyExistsException();
		}
	}

	@Transactional
	public Role updateRole(UUID establishmentId, String code, UpdateRoleCommand command, EmployeeActor actor) {
		requireScope(establishmentId, actor);
		String normalizedCode = normalizeCode(code);
		Role current = securityDirectoryRepository.findRoleByCodeAndEstablishmentId(normalizedCode, establishmentId)
			.orElseThrow(RoleNotFoundException::new);
		Role updated = buildRole(normalizedCode, command.name(), command.description(),
			command.permissionCodes(), current.establishmentId());
		return securityDirectoryRepository.updateRole(updated);
	}

	private Role buildRole(String code, String name, String description, List<String> permissionCodes,
		UUID establishmentId) {
		if (permissionCodes == null || permissionCodes.isEmpty()) {
			throw new InvalidRoleException("At least one permission is required");
		}
		Set<String> requestedCodes = new LinkedHashSet<>(permissionCodes);
		if (requestedCodes.size() != permissionCodes.size()) {
			throw new InvalidRoleException("Permission codes must be unique");
		}
		if (!java.util.Collections.disjoint(requestedCodes, PLATFORM_PERMISSIONS)) {
			throw new InvalidRoleException("Platform permissions cannot be assigned to establishment roles");
		}
		Map<String, Permission> availablePermissions = securityDirectoryRepository.findAllPermissions().stream()
			.filter(permission -> !PLATFORM_PERMISSIONS.contains(permission.code()))
			.collect(Collectors.toMap(Permission::code, Function.identity()));
		Set<Permission> permissions = requestedCodes.stream().map(availablePermissions::get)
			.collect(Collectors.toCollection(LinkedHashSet::new));
		if (permissions.contains(null)) {
			throw new InvalidRoleException("All role permissions must exist in the establishment permission catalog");
		}
		String normalizedName = required(name, "name", 100);
		String normalizedDescription = description == null || description.isBlank()
			? null : required(description, "description", 255);
		try {
			return new Role(code, normalizedName, normalizedDescription, true, permissions, establishmentId);
		} catch (IllegalArgumentException exception) {
			throw new InvalidRoleException(exception.getMessage());
		}
	}

	private void requireScope(UUID establishmentId, EmployeeActor actor) {
		if (actor.globalAdministrator()) {
			return;
		}
		if (!actor.establishmentAdministrator()) {
			throw new AccessDeniedException("Role management requires an establishment administrator");
		}
		employeeRepository.findByEmail(actor.username())
			.filter(Employee::enabled)
			.filter(Employee::establishmentAdmin)
			.filter(employee -> employee.userAccountId() != null)
			.filter(employee -> establishmentId.equals(employee.establishmentId()))
			.orElseThrow(() -> new AccessDeniedException("Role management is outside the caller's establishment"));
	}

	private String normalizeCode(String code) {
		String normalized = required(code, "code", 64).toUpperCase(Locale.ROOT);
		if (!CODE_PATTERN.matcher(normalized).matches()) {
			throw new InvalidRoleException("Role code must start with a letter and contain only uppercase letters, numbers, or underscores");
		}
		return normalized;
	}

	private String required(String value, String field, int maxLength) {
		if (value == null || value.isBlank() || value.strip().length() > maxLength) {
			throw new InvalidRoleException(field + " is required and must not exceed " + maxLength + " characters");
		}
		return value.strip();
	}
}