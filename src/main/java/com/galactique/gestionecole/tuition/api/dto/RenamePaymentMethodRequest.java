package com.galactique.gestionecole.tuition.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenamePaymentMethodRequest(@NotBlank @Size(max = 100) String name) {
}