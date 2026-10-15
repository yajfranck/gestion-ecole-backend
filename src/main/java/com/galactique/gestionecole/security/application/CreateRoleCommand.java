package com.galactique.gestionecole.security.application;

import java.util.List;

public record CreateRoleCommand(String code, String name, String description, List<String> permissionCodes) {
}