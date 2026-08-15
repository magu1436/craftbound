package com.magu1436.craftbound.occupations.blacksmith.carving.definition;

import com.magu1436.craftbound.occupations.blacksmith.carving.logic.CarvingGrid;
import net.minecraft.resources.ResourceLocation;

public record CarvingDefinitionSnapshot(ResourceLocation outputItemId, int ingredientCount,
    ResourceLocation requiredToolItemId, double removePerPass, String pathInterpolation,
    double removedUnitsPerDurability, double warningRetention,
    ResourceLocation breakEvaluatorId, double breakThreshold,
    ResourceLocation shapeEvaluatorId, CarvingGrid idealShape) {
    public CarvingDefinitionSnapshot {
        if (ingredientCount < 1 || removePerPass <= 0.0D || removedUnitsPerDurability <= 0.0D
            || warningRetention < 0.0D || warningRetention > 1.0D
            || breakThreshold < 0.0D || breakThreshold > 1.0D) {
            throw new IllegalArgumentException("invalid carving definition snapshot");
        }
        idealShape = idealShape.copy();
    }
}
