package com.galactique.gestionecole.schoolclasses.application;

import java.time.LocalDate;

public record CreateAcademicYearCommand(String name, LocalDate startsOn, LocalDate endsOn) {
}