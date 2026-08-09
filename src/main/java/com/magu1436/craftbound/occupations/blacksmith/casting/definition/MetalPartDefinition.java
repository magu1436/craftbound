package com.magu1436.craftbound.occupations.blacksmith.casting.definition;

import net.minecraft.resources.ResourceLocation;

public record MetalPartDefinition(
    ResourceLocation id, ResourceLocation metalId, int ingredientCount,
    ResourceLocation moldItemId, ResourceLocation outputItemId,
    FailureLumpDefinition failureLump, CoolingDefinition cooling,
    ForgingDefinition forging, PartQualityDefinition partQuality
) {
    public MetalPartDefinitionSnapshot snapshot() {
        return new MetalPartDefinitionSnapshot(id, metalId, ingredientCount, moldItemId,
            outputItemId, failureLump, cooling, forging, partQuality);
    }

    public record FailureLumpDefinition(ResourceLocation itemId, int count, int unitsPerItem) {}
    public record CoolingDefinition(long surfaceSolidTicks, long safeTicks,
        int minimumBreakOnHit, ResourceLocation evaluatorId) {}
    public record ForgingDefinition(double strengthMin, double strengthMax,
        double strengthPenaltyPerPoint, int idealHits, double hitCountPenalty,
        int breakOnHit, double strengthWeight, double hitCountWeight,
        ResourceLocation evaluatorId) {}
    public record PartQualityDefinition(double heatingWeight, double forgingWeight,
        ResourceLocation evaluatorId) {}
}
