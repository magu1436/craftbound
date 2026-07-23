package com.magu1436.craftbound.common;

import java.util.Objects;

import javax.annotation.Nonnull;

import com.magu1436.craftbound.Craftbound;

import net.minecraft.resources.ResourceLocation;

public class CraftboundUtilities {

    @Nonnull
    public static ResourceLocation createResourceLocation(@Nonnull String path) {
        return Objects.requireNonNull(
            ResourceLocation.tryBuild(Craftbound.MODID, path),
            "ResourceLocation is null"
        );
    }
}
