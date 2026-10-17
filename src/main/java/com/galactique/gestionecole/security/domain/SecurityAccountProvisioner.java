package com.galactique.gestionecole.security.domain;

import java.util.UUID;
import java.util.Set;

public interface SecurityAccountProvisioner {
	PasswordChangeResult changePassword(String username, String currentPassword, String newPassword);

	UUID createGuardianAccount(String username, String rawPassword);

	void addGuardianRole(UUID userAccountId);

	UUID createEmployeeAccount(String username, String rawPassword, Set<String> effectiveRoleCodes);

	void updateEmployeeAccount(UUID userAccountId, String username, Set<String> effectiveRoleCodes);

	void updateEmployeeRoles(UUID userAccountId, Set<String> effectiveRoleCodes);

	void disableEmployeeAccount(UUID userAccountId);
}
