package com.galactique.gestionecole.establishments.application;

import java.util.UUID;

import com.galactique.gestionecole.establishments.domain.Establishment;

public record OnboardedEstablishment(Establishment establishment, UUID administratorEmployeeId) {
}