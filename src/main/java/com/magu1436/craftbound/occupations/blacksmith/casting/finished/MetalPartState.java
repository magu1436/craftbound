package com.magu1436.craftbound.occupations.blacksmith.casting.finished;

import java.util.Objects;
import com.magu1436.craftbound.occupations.blacksmith.material.MetalVisualData;
import net.minecraft.resources.ResourceLocation;

public record MetalPartState(
    int version,
    ResourceLocation metalId,
    MetalVisualData visualData
) {
    public static final int CURRENT_VERSION = 1;

    public MetalPartState {
        Objects.requireNonNull(metalId, "metalId");
        Objects.requireNonNull(visualData, "visualData");
        if (version != CURRENT_VERSION) {
            throw new IllegalArgumentException("unsupported metal part state version: " + version);
        }
    }
}
