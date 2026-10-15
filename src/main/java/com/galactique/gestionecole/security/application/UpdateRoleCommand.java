package com.galactique.gestionecole.security.application;

import java.util.List;

public record UpdateRoleCommand(String code, String name, String description, List<String> permissionCodes) {
}