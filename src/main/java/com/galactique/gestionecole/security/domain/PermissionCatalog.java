package com.galactique.gestionecole.security.domain;

import java.util.Set;

/** Permissions that cannot be delegated through an establishment role or position. */
public final class PermissionCatalog {
	public static final Set<String> PLATFORM_ONLY = Set.of(
		"can_employee_assign_administrator", "can_user_read", "can_platform_role_read", "can_platform_role_create", "can_platform_role_update",
		"can_establishment_onboard", "can_establishment_delete",
		"can_license_offer_read", "can_license_offer_create", "can_license_assign",
		"can_license_suspend", "can_license_reactivate", "can_license_end", "can_license_event_read");

	private PermissionCatalog() { }

	public static boolean isPlatformOnly(String code) { return PLATFORM_ONLY.contains(code); }
}
