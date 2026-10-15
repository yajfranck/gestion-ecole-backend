package com.galactique.gestionecole.schoolclasses.application;

public record CreateGradeLevelCommand(String code, String name, int displayOrder) {
}