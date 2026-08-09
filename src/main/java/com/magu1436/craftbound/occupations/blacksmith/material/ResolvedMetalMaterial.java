package com.magu1436.craftbound.occupations.blacksmith.material;

import java.util.Objects;

import net.minecraft.resources.ResourceLocation;

public record ResolvedMetalMaterial(
    ResourceLocation metalId,
    int amountPerItem
) {
    public ResolvedMetalMaterial {
        Objects.requireNonNull(metalId, "metalId");
        if (amountPerItem <= 0) {
            throw new IllegalArgumentException(
                "amountPerItem must be greater than zero"
            );
        }
    }
}
