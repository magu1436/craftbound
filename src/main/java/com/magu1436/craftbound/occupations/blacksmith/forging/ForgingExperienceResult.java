package com.magu1436.craftbound.occupations.blacksmith.forging;

import java.util.Objects;
import java.util.UUID;

public record ForgingExperienceResult(
    UUID resultId,
    UUID operatorId,
    int materialUnits,
    boolean success,
    boolean permanentMaterialLoss
) {
    public ForgingExperienceResult {
        Objects.requireNonNull(resultId, "resultId");
        Objects.requireNonNull(operatorId, "operatorId");
        if (materialUnits < 1) {
            throw new IllegalArgumentException("materialUnits must be positive");
        }
    }
}
