package com.magu1436.craftbound.occupations.blacksmith.material;

import net.minecraft.resources.ResourceLocation;

public record MetalVisualData(Integer explicitRgb, ResourceLocation representativeItemId) {
    public MetalVisualData {
        if (explicitRgb != null && (explicitRgb < 0 || explicitRgb > 0xFFFFFF)) {
            throw new IllegalArgumentException("explicitRgb must be a 24-bit RGB value");
        }
        if (explicitRgb == null && representativeItemId == null) {
            throw new IllegalArgumentException(
                "explicitRgb or representativeItemId is required"
            );
        }
    }
}
