package com.galactique.gestionecole.schoolclasses.application;

public record UpdateGradeLevelCommand(String code, String name, Integer displayOrder) {
}