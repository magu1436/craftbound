package com.magu1436.craftbound.common;

import java.util.Objects;

import com.magu1436.craftbound.Craftbound;

import net.minecraft.resources.ResourceLocation;

public final class CraftboundUtilities {
    private CraftboundUtilities() {
    }

    public static ResourceLocation createResourceLocation(String path) {
        return Objects.requireNonNull(
            ResourceLocation.tryBuild(Craftbound.MODID, path),
            "ResourceLocation is null"
        );
    }
}
