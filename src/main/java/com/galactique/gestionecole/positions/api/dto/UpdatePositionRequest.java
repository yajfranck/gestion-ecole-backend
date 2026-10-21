package com.galactique.gestionecole.positions.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePositionRequest(@NotBlank @Size(max = 100) String name) {
}
