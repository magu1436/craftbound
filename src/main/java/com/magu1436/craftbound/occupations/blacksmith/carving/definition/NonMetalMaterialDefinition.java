package com.magu1436.craftbound.occupations.blacksmith.carving.definition;

import net.minecraft.resources.ResourceLocation;

public record NonMetalMaterialDefinition(ResourceLocation id, ResourceLocation toolItemId,
    double removePerPass, String pathInterpolation, double removedUnitsPerDurability,
    ResourceLocation carvingTexture) {}
