package com.galactique.gestionecole.schoolclasses.application;

import java.time.LocalDate;

public record UpdateAcademicYearCommand(String name, LocalDate startsOn, LocalDate endsOn) {
}