package com.galactique.gestionecole.security.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SecurityDirectoryRepository {
	List<UserAccount> findAllUsers();

	List<Role> findAllRoles();

	List<Role> findRolesForEstablishment(UUID establishmentId);

	Optional<Role> findRoleByCode(String code);

	Optional<Role> findRoleByCodeAndEstablishmentId(String code, UUID establishmentId);

	Role createRole(Role role);

	Role updateRole(Role role);

	List<Permission> findAllPermissions();
}