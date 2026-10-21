package com.galactique.gestionecole.schoolclasses.application;

public record CreateClassroomCommand(String code, String name, int maxStudents) { }