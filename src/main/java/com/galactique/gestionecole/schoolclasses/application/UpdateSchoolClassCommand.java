package com.galactique.gestionecole.schoolclasses.application;

import java.util.UUID;

public record UpdateSchoolClassCommand(UUID gradeLevelId, String code, String name) {
}