package com.galactique.gestionecole.schoolclasses.application;

import java.time.LocalDate;

public record CreateAcademicPeriodCommand(String name, LocalDate startsOn, LocalDate endsOn) {
}