package com.galactique.gestionecole.tuition.domain;

import java.util.UUID;

public record TuitionFeeTypeDraft(UUID establishmentId, String code, String name) {
}