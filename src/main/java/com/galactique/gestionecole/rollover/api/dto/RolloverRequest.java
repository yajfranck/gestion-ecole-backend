package com.galactique.gestionecole.rollover.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.galactique.gestionecole.rollover.application.RolloverCommand;

public record RolloverRequest(@NotNull UUID sourceAcademicYearId,
    @NotEmpty @Size(max = 200) List<@NotNull UUID> classroomIds, Boolean includeAnnualFees,
    @Size(max = 200) Map<@NotNull UUID, @NotNull @Valid FeeOverrideRequest> feeOverrides) {
    public record FeeOverrideRequest(LocalDate dueDate, UUID feeTypeId) { }
    public RolloverCommand toCommand() {
        return new RolloverCommand(sourceAcademicYearId, classroomIds, !Boolean.FALSE.equals(includeAnnualFees),
            feeOverrides == null ? Map.of() : feeOverrides.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                e -> new RolloverCommand.FeeOverride(e.getValue().dueDate(), e.getValue().feeTypeId()))));
    }
}
