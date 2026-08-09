package com.magu1436.craftbound.occupations.blacksmith.casting.lump;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record MetalLumpState(int version, ResourceLocation metalId, int unitsPerItem) {
    public static final int CURRENT_VERSION = 1;
    public MetalLumpState {
        Objects.requireNonNull(metalId);
        if (version != CURRENT_VERSION || unitsPerItem < 1)
            throw new IllegalArgumentException("invalid metal lump state");
    }
}
