package com.galactique.gestionecole.schoolclasses.application;

import java.util.UUID;

public record CreateSchoolClassCommand(UUID gradeLevelId, String code, String name) {
}