package com.galactique.gestionecole.timetables.api.dto;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NonWorkingDaysRequest(@NotNull @Size(max = 730) List<@Valid @NotNull Day> days) {
	public record Day(@NotNull LocalDate date, @NotBlank @Size(max = 100) String name) { }
}
