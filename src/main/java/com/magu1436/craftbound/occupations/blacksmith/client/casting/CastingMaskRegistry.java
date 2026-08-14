package com.magu1436.craftbound.occupations.blacksmith.client.casting;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;

public final class CastingMaskRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile Map<ResourceLocation, CastingMaskDefinition> definitionsByMold =
        Map.of();
    private static volatile Set<ResourceLocation> reportedMissingMolds =
        ConcurrentHashMap.newKeySet();

    private CastingMaskRegistry() {
    }

    public static Optional<CastingMaskDefinition> findByMold(
        ResourceLocation moldItemId
    ) {
        CastingMaskDefinition definition = definitionsByMold.get(moldItemId);
        if (definition == null && reportedMissingMolds.add(moldItemId)) {
            LOGGER.warn("No casting mask is defined for mold {}", moldItemId);
        }
        return Optional.ofNullable(definition);
    }

    static void replace(Map<ResourceLocation, CastingMaskDefinition> definitions) {
        definitionsByMold = Map.copyOf(definitions);
        reportedMissingMolds = ConcurrentHashMap.newKeySet();
    }
}
