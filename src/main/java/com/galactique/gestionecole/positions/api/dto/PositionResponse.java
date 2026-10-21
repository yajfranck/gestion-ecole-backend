package com.galactique.gestionecole.positions.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.galactique.gestionecole.positions.domain.Position;

public record PositionResponse(
	UUID id,
	UUID establishmentId,
	String name,
	boolean enabled,
	List<String> roleCodes,
	Instant createdAt,
	Instant updatedAt) {
	public static PositionResponse from(Position position) {
		return new PositionResponse(position.id(), position.establishmentId(), position.name(), position.enabled(),
			position.roleCodes().stream().sorted().toList(), position.createdAt(), position.updatedAt());
	}
}
