package com.magu1436.craftbound.occupations.blacksmith.casting.definition;

import net.minecraft.resources.ResourceLocation;
import com.magu1436.craftbound.occupations.blacksmith.casting.definition.MetalPartDefinition.*;

public record MetalPartDefinitionSnapshot(
    ResourceLocation definitionId, ResourceLocation metalId, int ingredientCount,
    ResourceLocation moldItemId, ResourceLocation outputItemId,
    FailureLumpDefinition failureLump, CoolingDefinition cooling,
    ForgingDefinition forging, PartQualityDefinition partQuality
) {}
