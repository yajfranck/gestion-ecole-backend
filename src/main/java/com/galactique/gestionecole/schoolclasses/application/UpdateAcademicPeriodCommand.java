package com.galactique.gestionecole.schoolclasses.application;

import java.time.LocalDate;

public record UpdateAcademicPeriodCommand(String name, LocalDate startsOn, LocalDate endsOn) {
}