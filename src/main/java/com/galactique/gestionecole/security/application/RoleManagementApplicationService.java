package com.galactique.gestionecole.security.application;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.security.domain.Permission;
import com.galactique.gestionecole.security.domain.Role;
import com.galactique.gestionecole.security.domain.SecurityDirectoryRepository;

@Service
public class RoleManagementApplicationService {
	private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]{0,63}");
	private final SecurityDirectoryRepository securityDirectoryRepository;

	public RoleManagementApplicationService(SecurityDirectoryRepository securityDirectoryRepository) {
		this.securityDirectoryRepository = securityDirectoryRepository;
	}

	@Transactional
	public Role create(CreateRoleCommand command) {
		String code = normalizeCode(command.code());
		if (securityDirectoryRepository.findRoleByCode(code).isPresent()) {
			throw new RoleAlreadyExistsException();
		}
		Role role = buildRole(code, command.name(), command.description(), command.permissionCodes(), true);
		try {
			return securityDirectoryRepository.createRole(role);
		} catch (DataIntegrityViolationException exception) {
			throw new RoleAlreadyExistsException();
		}
	}

	@Transactional
	public Role update(String code, UpdateRoleCommand command) {
		String normalizedCode = normalizeCode(code);
		Role current = securityDirectoryRepository.findRoleByCode(normalizedCode)
			.orElseThrow(RoleNotFoundException::new);
		Set<String> permissionCodes = new LinkedHashSet<>(command.permissionCodes());
		if ("ADMIN".equals(normalizedCode) && permissionCodes.contains("ADMIN_ACCESS")) {
			throw new InvalidRoleException("The ADMIN role cannot have ADMIN_ACCESS");
		}
		Role updated = buildRole(normalizedCode, command.name(), command.description(),
			command.permissionCodes(), current.enabled());
		return securityDirectoryRepository.updateRole(updated);
	}

	private Role buildRole(String code, String name, String description,
		List<String> permissionCodes, boolean enabled) {
		if (permissionCodes == null || permissionCodes.isEmpty()) {
			throw new InvalidRoleException("At least one permission is required");
		}
		Set<String> requestedCodes = new LinkedHashSet<>(permissionCodes);
		if (requestedCodes.size() != permissionCodes.size()) {
			throw new InvalidRoleException("Permission codes must be unique");
		}
		Map<String, Permission> availablePermissions = securityDirectoryRepository.findAllPermissions().stream()
			.collect(Collectors.toMap(Permission::code, Function.identity()));
		Set<Permission> permissions = requestedCodes.stream().map(availablePermissions::get)
			.collect(Collectors.toCollection(LinkedHashSet::new));
		if (permissions.contains(null)) {
			throw new InvalidRoleException("All permission codes must exist in the permission catalog");
		}
		String normalizedName = required(name, "name", 100);
		String normalizedDescription = description == null || description.isBlank()
			? null : required(description, "description", 255);
		try {
			return new Role(code, normalizedName, normalizedDescription, enabled, permissions);
		} catch (IllegalArgumentException exception) {
			throw new InvalidRoleException(exception.getMessage());
		}
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