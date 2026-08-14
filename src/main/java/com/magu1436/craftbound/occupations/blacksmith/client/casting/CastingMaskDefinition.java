package com.magu1436.craftbound.occupations.blacksmith.client.casting;

import java.util.Objects;

import net.minecraft.resources.ResourceLocation;

public record CastingMaskDefinition(
    ResourceLocation id,
    ResourceLocation moldItemId,
    int width,
    int height,
    CastingMaskGeometry geometry
) {
    public CastingMaskDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(moldItemId, "moldItemId");
        Objects.requireNonNull(geometry, "geometry");
    }
}
