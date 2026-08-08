package com.magu1436.craftbound.occupations.explorer.registry;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.minecraft.resources.ResourceLocation;

/** JSON解析済みで、ゲームレジストリ解決前の定義集合。 */
public record ExplorerDiscoveryDefinitions(
    Map<ResourceLocation, ExperienceTier> experienceTiers,
    List<BiomeDiscoveryRule> biomeRules,
    List<StructureDiscoveryRule> structureRules,
    List<DimensionDiscoveryRule> dimensionRules,
    ExplorerDefinitionErrors errors
) {
    public ExplorerDiscoveryDefinitions {
        experienceTiers = Map.copyOf(experienceTiers);
        biomeRules = List.copyOf(biomeRules);
        structureRules = List.copyOf(structureRules);
        dimensionRules = List.copyOf(dimensionRules);
        Objects.requireNonNull(errors, "errors is null");
    }
}
