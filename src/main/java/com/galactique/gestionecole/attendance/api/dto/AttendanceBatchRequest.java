package com.galactique.gestionecole.attendance.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record AttendanceBatchRequest(@NotEmpty List<@Valid AttendanceEntryRequest> entries) {
}