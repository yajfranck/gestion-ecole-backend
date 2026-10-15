package com.galactique.gestionecole.security.domain;

import java.util.List;
import java.util.Optional;

public interface SecurityDirectoryRepository {
	List<UserAccount> findAllUsers();

	List<Role> findAllRoles();

	Optional<Role> findRoleByCode(String code);

	Role createRole(Role role);

	Role updateRole(Role role);

	List<Permission> findAllPermissions();
}