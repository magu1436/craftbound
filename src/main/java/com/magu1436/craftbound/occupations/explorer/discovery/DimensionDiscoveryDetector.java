package com.magu1436.craftbound.occupations.explorer.discovery;

import java.util.Optional;

import com.magu1436.craftbound.occupations.explorer.data.ExplorerDiscoveryData;
import com.magu1436.craftbound.occupations.explorer.registry.ExplorerDiscoveryRegistry;
import com.magu1436.craftbound.occupations.explorer.registry.ExplorerDiscoverySnapshot.ResolvedDimensionRule;
import com.magu1436.craftbound.registry.CraftboundCapabilities;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** 現在の未発見ディメンションを検出する。 */
public final class DimensionDiscoveryDetector {
    private final ExplorerDiscoveryRegistry registry;

    public DimensionDiscoveryDetector(ExplorerDiscoveryRegistry registry) {
        this.registry = registry;
    }

    public Optional<DiscoveryTarget.Dimension> detect(ServerPlayer player) {
        ResourceLocation dimensionId = player.serverLevel()
            .dimension()
            .location();
        ExplorerDiscoveryData data = player.getCapability(
            CraftboundCapabilities.EXPLORER_DISCOVERY_DATA
        ).resolve().orElse(null);
        if (
            data == null
                || data.hasDiscoveredDimension(dimensionId.toString())
        ) {
            return Optional.empty();
        }

        ResolvedDimensionRule rule = registry.findDimensionRule(
            dimensionId
        ).orElse(null);
        if (rule == null || !rule.enabled()) {
            return Optional.empty();
        }
        return Optional.of(new DiscoveryTarget.Dimension(
            dimensionId,
            rule.xp(),
            rule.dwellTicks(),
            rule.translationKey().orElse(null)
        ));
    }
}
