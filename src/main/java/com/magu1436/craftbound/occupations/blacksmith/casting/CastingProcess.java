package com.magu1436.craftbound.occupations.blacksmith.casting;

import java.util.Objects;
import java.util.UUID;

import com.magu1436.craftbound.occupations.blacksmith.casting.definition.MetalPartDefinitionSnapshot;
import com.magu1436.craftbound.occupations.blacksmith.material.MetalVisualData;

import net.minecraft.resources.ResourceLocation;

public record CastingProcess(
    int version,
    UUID processId,
    UUID operatorId,
    ResourceLocation partDefinitionId,
    ResourceLocation metalId,
    int metalAmount,
    long heatingTicks,
    int heatingScore,
    long coolingTicks,
    boolean surfaceSolidificationNotified,
    MetalPartDefinitionSnapshot definitionSnapshot,
    MetalVisualData visualData
) {
    public static final int CURRENT_VERSION = 2;

    public CastingProcess {
        Objects.requireNonNull(processId, "processId");
        Objects.requireNonNull(operatorId, "operatorId");
        Objects.requireNonNull(partDefinitionId, "partDefinitionId");
        Objects.requireNonNull(metalId, "metalId");
        Objects.requireNonNull(definitionSnapshot, "definitionSnapshot");
        Objects.requireNonNull(visualData, "visualData");

        boolean valid = version == CURRENT_VERSION
            && metalAmount >= 1
            && heatingTicks >= 0L
            && heatingScore >= 0
            && heatingScore <= 100
            && coolingTicks >= 0L
            && partDefinitionId.equals(definitionSnapshot.definitionId())
            && metalAmount == definitionSnapshot.ingredientCount();
        if (!valid) {
            throw new IllegalArgumentException(
                "casting process invariants are not satisfied"
            );
        }
    }
}
