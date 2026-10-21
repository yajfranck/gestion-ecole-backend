package com.galactique.gestionecole.attendance.api.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record AttendanceBatchRequest(@NotNull UUID classroomId,
	@NotEmpty List<@NotNull @Valid AttendanceEntryRequest> entries) {
}
