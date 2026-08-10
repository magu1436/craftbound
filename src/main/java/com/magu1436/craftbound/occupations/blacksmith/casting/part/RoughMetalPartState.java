package com.magu1436.craftbound.occupations.blacksmith.casting.part;

import java.util.Objects;
import java.util.UUID;
import com.magu1436.craftbound.occupations.blacksmith.casting.definition.MetalPartDefinitionSnapshot;
import com.magu1436.craftbound.occupations.blacksmith.material.MetalVisualData;
import net.minecraft.resources.ResourceLocation;

public record RoughMetalPartState(int version, UUID castingResultId, UUID castingOperatorId,
    ResourceLocation definitionId, ResourceLocation metalId, ResourceLocation outputItemId,
    int ingredientCount, long heatingTicks, int heatingScore, long coolingTicksAtRemoval,
    int effectiveBreakOnHit, MetalPartDefinitionSnapshot definitionSnapshot,
    MetalVisualData visualData) {
    public static final int CURRENT_VERSION = 1;
    public RoughMetalPartState {
        Objects.requireNonNull(castingResultId); Objects.requireNonNull(castingOperatorId);
        Objects.requireNonNull(definitionId); Objects.requireNonNull(metalId);
        Objects.requireNonNull(outputItemId); Objects.requireNonNull(definitionSnapshot);
        Objects.requireNonNull(visualData);
        boolean valid = version == CURRENT_VERSION && ingredientCount >= 1 && heatingTicks >= 0
            && heatingScore >= 0 && heatingScore <= 100 && coolingTicksAtRemoval >= 0
            && effectiveBreakOnHit >= 1 && effectiveBreakOnHit <= definitionSnapshot.forging().breakOnHit()
            && definitionId.equals(definitionSnapshot.definitionId()) && metalId.equals(definitionSnapshot.metalId())
            && outputItemId.equals(definitionSnapshot.outputItemId()) && ingredientCount == definitionSnapshot.ingredientCount()
            && definitionSnapshot.moldItemId() != null;
        if (!valid) throw new IllegalArgumentException("rough metal part invariants are not satisfied");
    }
}
