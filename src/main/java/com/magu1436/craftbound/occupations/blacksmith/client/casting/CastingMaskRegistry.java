package com.magu1436.craftbound.occupations.blacksmith.client.casting;

import java.util.Map;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;

public final class CastingMaskRegistry {
    private static volatile Map<ResourceLocation, CastingMaskDefinition> definitionsByMold =
        Map.of();

    private CastingMaskRegistry() {
    }

    public static Optional<CastingMaskDefinition> findByMold(
        ResourceLocation moldItemId
    ) {
        return Optional.ofNullable(definitionsByMold.get(moldItemId));
    }

    static void replace(Map<ResourceLocation, CastingMaskDefinition> definitions) {
        definitionsByMold = Map.copyOf(definitions);
    }
}
