package com.galactique.gestionecole.rollover.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RolloverCommand(UUID sourceAcademicYearId, List<UUID> classroomIds, boolean includeAnnualFees,
    Map<UUID, FeeOverride> feeOverrides) {
    public record FeeOverride(LocalDate dueDate, UUID feeTypeId) { }
}
