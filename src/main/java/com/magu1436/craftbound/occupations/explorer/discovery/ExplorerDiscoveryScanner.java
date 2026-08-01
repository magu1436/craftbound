package com.magu1436.craftbound.occupations.explorer.discovery;

import java.util.Map;
import java.util.Optional;

import net.minecraft.server.level.ServerPlayer;

/** そのtickでプレイヤーが滞在している発見対象をまとめる。 */
public final class ExplorerDiscoveryScanner {
    private final BiomeDiscoveryDetector biomeDetector;
    private final DimensionDiscoveryDetector dimensionDetector;

    public ExplorerDiscoveryScanner(
        BiomeDiscoveryDetector biomeDetector,
        DimensionDiscoveryDetector dimensionDetector
    ) {
        this.biomeDetector = biomeDetector;
        this.dimensionDetector = dimensionDetector;
    }

    public DiscoveryScanResult scan(ServerPlayer player) {
        return new DiscoveryScanResult(
            biomeDetector.detect(player),
            dimensionDetector.detect(player),
            Map.of()
        );
    }

    public record DiscoveryScanResult(
        Optional<DiscoveryTarget.Biome> biome,
        Optional<DiscoveryTarget.Dimension> dimension,
        Map<StructureInstanceKey, DiscoveryTarget.Structure> structures
    ) {
        public DiscoveryScanResult {
            biome = Optional.ofNullable(biome).orElseGet(Optional::empty);
            dimension = Optional.ofNullable(dimension)
                .orElseGet(Optional::empty);
            structures = Map.copyOf(structures);
        }
    }
}
