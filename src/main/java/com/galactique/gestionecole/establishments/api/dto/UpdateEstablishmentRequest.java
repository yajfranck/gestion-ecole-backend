package com.galactique.gestionecole.establishments.api.dto;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.MissingNode;
import com.galactique.gestionecole.establishments.application.UpdateEstablishmentCommand;

public record UpdateEstablishmentRequest(JsonNode code, JsonNode name) {
	public UpdateEstablishmentRequest {
		code = code == null ? MissingNode.getInstance() : code;
		name = name == null ? MissingNode.getInstance() : name;
	}

	public UpdateEstablishmentCommand toCommand() {
		String normalizedCode = readOptionalText(code, "code", 40);
		String normalizedName = readOptionalText(name, "name", 200);
		if (normalizedCode == null && normalizedName == null) {
			throw new IllegalArgumentException("At least one establishment field must be provided");
		}
		return new UpdateEstablishmentCommand(normalizedCode, normalizedName);
	}

	private String readOptionalText(JsonNode value, String field, int maxLength) {
		if (value instanceof MissingNode) {
			return null;
		}
		if (!value.isString()) {
			throw new IllegalArgumentException(field + " must be a non-empty string");
		}
		String normalized = value.stringValue().strip();
		if (normalized.isEmpty() || normalized.length() > maxLength) {
			throw new IllegalArgumentException(field + " must contain between 1 and " + maxLength + " characters");
		}
		return normalized;
	}
}