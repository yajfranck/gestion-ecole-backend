package com.galactique.gestionecole.security.domain;

import java.util.Objects;

public record Permission(String code, String description) {
	public Permission {
		Objects.requireNonNull(code, "code must not be null");
		if (code.length() > 64
			|| !isConventionPermissionCode(code)) {
			throw new IllegalArgumentException("Permission code must follow can_<entity>_<action>");
		}
		Objects.requireNonNull(description, "description must not be null");
		if (description.isBlank() || description.length() > 255) {
			throw new IllegalArgumentException("Permission description must contain between 1 and 255 characters");
		}
	}

	private static boolean isConventionPermissionCode(String code) {
		String[] codeSegments = code.split("_", -1);
		if (codeSegments.length < 3 || !"can".equals(codeSegments[0])) {
			return false;
		}
		for (int segmentIndex = 1; segmentIndex < codeSegments.length; segmentIndex++) {
			String segment = codeSegments[segmentIndex];
			if (segment.isEmpty()) {
				return false;
			}
			for (int characterIndex = 0; characterIndex < segment.length(); characterIndex++) {
				char character = segment.charAt(characterIndex);
				if (!(character >= 'a' && character <= 'z') && !(character >= '0' && character <= '9')) {
					return false;
				}
			}
		}
		return true;
	}
}
