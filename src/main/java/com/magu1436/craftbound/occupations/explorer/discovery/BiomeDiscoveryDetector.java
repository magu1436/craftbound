package com.magu1436.craftbound.occupations.explorer.discovery;

import java.util.Optional;

import com.magu1436.craftbound.occupations.explorer.data.ExplorerDiscoveryData;
import com.magu1436.craftbound.occupations.explorer.registry.ExplorerDiscoveryRegistry;
import com.magu1436.craftbound.occupations.explorer.registry.ExplorerDiscoverySnapshot.ResolvedBiomeRule;
import com.magu1436.craftbound.registry.CraftboundCapabilities;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;

/** 現在位置の未発見バイオームを検出する。 */
public final class BiomeDiscoveryDetector {
    private final ExplorerDiscoveryRegistry registry;

    public BiomeDiscoveryDetector(ExplorerDiscoveryRegistry registry) {
        this.registry = registry;
    }

    public Optional<DiscoveryTarget.Biome> detect(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Holder<Biome> holder = level.getBiome(player.blockPosition());
        Optional<ResourceKey<Biome>> biomeKey = holder.unwrapKey();
        if (biomeKey.isEmpty()) {
            return Optional.empty();
        }

        ResourceLocation biomeId = biomeKey.get().location();
        ExplorerDiscoveryData data = player.getCapability(
            CraftboundCapabilities.EXPLORER_DISCOVERY_DATA
        ).resolve().orElse(null);
        if (data == null || data.hasDiscoveredBiome(biomeId.toString())) {
            return Optional.empty();
        }

        ResolvedBiomeRule rule = registry.findBiomeRule(biomeId).orElse(null);
        if (rule == null || !rule.enabled()) {
            return Optional.empty();
        }
        return Optional.of(new DiscoveryTarget.Biome(
            biomeId,
            rule.xp(),
            rule.dwellTicks(),
            rule.translationKey().orElse(null)
        ));
    }
}
