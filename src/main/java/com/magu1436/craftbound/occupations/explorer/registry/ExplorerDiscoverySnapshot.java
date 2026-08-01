package com.magu1436.craftbound.occupations.explorer.registry;

import java.util.Map;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;

/** 検証・タグ展開・経験値解決が完了した不変の実行時定義。 */
public record ExplorerDiscoverySnapshot(
    Map<ResourceLocation, ExperienceTier> experienceTiers,
    Map<ResourceLocation, ResolvedBiomeRule> biomeRules,
    Map<ResourceLocation, ResolvedStructureRule> structureRules,
    Map<ResourceLocation, ResolvedDimensionRule> dimensionRules
) {
    public ExplorerDiscoverySnapshot {
        experienceTiers = Map.copyOf(experienceTiers);
        biomeRules = Map.copyOf(biomeRules);
        structureRules = Map.copyOf(structureRules);
        dimensionRules = Map.copyOf(dimensionRules);
    }

    public static ExplorerDiscoverySnapshot empty() {
        return new ExplorerDiscoverySnapshot(
            Map.of(), Map.of(), Map.of(), Map.of()
        );
    }

    public Optional<ResolvedBiomeRule> findBiome(ResourceLocation biomeId) {
        ResolvedBiomeRule explicit = biomeRules.get(biomeId);
        if (explicit != null) {
            return Optional.of(explicit);
        }
        ExperienceTier common = experienceTiers.get(ExperienceSpec.DEFAULT);
        return common == null
            ? Optional.empty()
            : Optional.of(new ResolvedBiomeRule(
                biomeId,
                true,
                common.xp(),
                BiomeDiscoveryRule.DEFAULT_DWELL_TICKS,
                Optional.empty()
            ));
    }

    public Optional<ResolvedStructureRule> findStructure(
        ResourceLocation structureId
    ) {
        return Optional.ofNullable(structureRules.get(structureId));
    }

    public Optional<ResolvedDimensionRule> findDimension(
        ResourceLocation dimensionId
    ) {
        return Optional.ofNullable(dimensionRules.get(dimensionId));
    }

    public record ResolvedBiomeRule(
        ResourceLocation biomeId,
        boolean enabled,
        int xp,
        int dwellTicks,
        Optional<String> translationKey
    ) {
        public static ResolvedBiomeRule disabled(ResourceLocation biomeId) {
            return new ResolvedBiomeRule(
                biomeId, false, 0, 0, Optional.empty()
            );
        }
    }

    public record ResolvedStructureRule(
        ResourceLocation structureId,
        boolean enabled,
        int xp,
        int dwellTicks,
        int maxDiscoveries,
        Optional<String> translationKey
    ) {
        public static ResolvedStructureRule disabled(
            ResourceLocation structureId
        ) {
            return new ResolvedStructureRule(
                structureId, false, 0, 0, 0, Optional.empty()
            );
        }
    }

    public record ResolvedDimensionRule(
        ResourceLocation dimensionId,
        boolean enabled,
        int xp,
        int dwellTicks,
        Optional<String> translationKey
    ) {
        public static ResolvedDimensionRule disabled(
            ResourceLocation dimensionId
        ) {
            return new ResolvedDimensionRule(
                dimensionId, false, 0, 0, Optional.empty()
            );
        }
    }
}
