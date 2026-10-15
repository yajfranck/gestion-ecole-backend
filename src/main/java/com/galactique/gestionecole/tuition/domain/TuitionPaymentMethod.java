package com.galactique.gestionecole.tuition.domain;

import java.util.UUID;

public record TuitionPaymentMethod(UUID id, UUID establishmentId, String code, String name, boolean enabled) {
}