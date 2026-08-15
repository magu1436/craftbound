package com.magu1436.craftbound.occupations.blacksmith.carving;

import java.util.Objects;
import java.util.UUID;

public record CarvingExperienceResult(UUID resultId, UUID operatorId, int ingredientCount,
    boolean success, boolean permanentMaterialLoss) {
    public CarvingExperienceResult {
        Objects.requireNonNull(resultId, "resultId"); Objects.requireNonNull(operatorId, "operatorId");
        if (ingredientCount < 1) throw new IllegalArgumentException("ingredientCount must be positive");
    }
}
