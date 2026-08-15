package com.magu1436.craftbound.occupations.blacksmith.carving;

import java.util.Objects;
import java.util.UUID;

public record CarvingExperienceResult(
    UUID resultId,
    UUID operatorId,
    boolean success,
    boolean permanentMaterialLoss
) {
    public CarvingExperienceResult {
        Objects.requireNonNull(resultId, "resultId");
        Objects.requireNonNull(operatorId, "operatorId");
    }
}
