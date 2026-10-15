package com.galactique.gestionecole.security.infrastructure.persistence;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.galactique.gestionecole.security.domain.Permission;
import com.galactique.gestionecole.security.domain.Role;
import com.galactique.gestionecole.security.domain.UserAccount;

@Component
public class SecurityPersistenceMapper {
	public UserAccount toDomain(UserAccountEntity entity) {
		Set<Role> roles = entity.getRoles().stream()
			.map(this::toDomain)
			.collect(Collectors.toSet());
		return new UserAccount(
			entity.getId(),
			entity.getUsername(),
			entity.getPasswordHash(),
			entity.isEnabled(),
			entity.isLocked(),
			entity.getCreatedAt(),
			entity.getUpdatedAt(),
			entity.getLastLoginAt(),
			roles);
	}

	public Role toDomain(RoleEntity entity) {
		Set<Permission> permissions = entity.getPermissions().stream()
			.map(this::toDomain)
			.collect(Collectors.toSet());
		return new Role(
			entity.getCode(),
			entity.getName(),
			entity.getDescription(),
			entity.isEnabled(),
			permissions,
			entity.getEstablishmentId());
	}

	public Permission toDomain(PermissionEntity entity) {
		return new Permission(entity.getCode(), entity.getDescription());
	}
}