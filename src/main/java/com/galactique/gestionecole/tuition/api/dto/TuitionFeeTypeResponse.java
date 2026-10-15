package com.galactique.gestionecole.tuition.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.galactique.gestionecole.tuition.domain.TuitionFeeType;

public record TuitionFeeTypeResponse(UUID id, UUID establishmentId, String code,
	String name, boolean enabled, Instant createdAt, Instant updatedAt) {
	public static TuitionFeeTypeResponse from(TuitionFeeType type) {
		return new TuitionFeeTypeResponse(type.id(), type.establishmentId(), type.code(),
			type.name(), type.enabled(), type.createdAt(), type.updatedAt());
	}
}