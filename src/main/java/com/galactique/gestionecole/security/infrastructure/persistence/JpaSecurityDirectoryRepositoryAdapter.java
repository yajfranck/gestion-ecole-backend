package com.galactique.gestionecole.security.infrastructure.persistence;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.galactique.gestionecole.security.domain.Permission;
import com.galactique.gestionecole.security.domain.Role;
import com.galactique.gestionecole.security.domain.SecurityDirectoryRepository;
import com.galactique.gestionecole.security.domain.UserAccount;

@Repository
public class JpaSecurityDirectoryRepositoryAdapter implements SecurityDirectoryRepository {
	private final UserAccountJpaRepository userAccountJpaRepository;
	private final RoleJpaRepository roleJpaRepository;
	private final PermissionJpaRepository permissionJpaRepository;
	private final SecurityPersistenceMapper mapper;

	public JpaSecurityDirectoryRepositoryAdapter(
		UserAccountJpaRepository userAccountJpaRepository,
		RoleJpaRepository roleJpaRepository,
		PermissionJpaRepository permissionJpaRepository,
		SecurityPersistenceMapper mapper) {
		this.userAccountJpaRepository = userAccountJpaRepository;
		this.roleJpaRepository = roleJpaRepository;
		this.permissionJpaRepository = permissionJpaRepository;
		this.mapper = mapper;
	}

	@Override
	@Transactional(readOnly = true)
	public List<UserAccount> findAllUsers() {
		return userAccountJpaRepository.findAll().stream().map(mapper::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Role> findAllRoles() {
		return roleJpaRepository.findAll().stream().map(mapper::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public java.util.Optional<Role> findRoleByCode(String code) {
		return roleJpaRepository.findByCode(code).map(mapper::toDomain);
	}

	@Override
	@Transactional
	public Role createRole(Role role) {
		Set<PermissionEntity> permissions = resolvePermissionEntities(role);
		RoleEntity entity = new RoleEntity(role.code(), role.name(), role.description(), role.enabled(), permissions);
		return mapper.toDomain(roleJpaRepository.saveAndFlush(entity));
	}

	@Override
	@Transactional
	public Role updateRole(Role role) {
		RoleEntity entity = roleJpaRepository.findByCode(role.code()).orElseThrow();
		entity.updateDetails(role.name(), role.description(), resolvePermissionEntities(role));
		return mapper.toDomain(entity);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Permission> findAllPermissions() {
		return permissionJpaRepository.findAll().stream().map(mapper::toDomain).toList();
	}

	private Set<PermissionEntity> resolvePermissionEntities(Role role) {
		List<PermissionEntity> entities = permissionJpaRepository.findAllByCodeIn(
			role.permissions().stream().map(Permission::code).toList());
		if (entities.size() != role.permissions().size()) {
			throw new IllegalArgumentException("All role permissions must exist in the permission catalog");
		}
		return entities.stream().collect(Collectors.toSet());
	}
}