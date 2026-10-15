package com.galactique.gestionecole.tuition.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record ReplacePaymentMethodsRequest(@NotEmpty List<@Valid PaymentMethodInputRequest> methods) {
}