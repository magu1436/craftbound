package com.magu1436.craftbound.occupations.explorer.discovery;

import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;

/** 滞在候補として共通に扱う、解決済みの発見対象。 */
public sealed interface DiscoveryTarget
    permits DiscoveryTarget.Biome,
        DiscoveryTarget.Dimension,
        DiscoveryTarget.Structure {

    DiscoveryType type();

    ResourceLocation targetId();

    int xp();

    int dwellTicks();

    String candidateKey();

    enum DiscoveryType {
        BIOME,
        STRUCTURE,
        DIMENSION
    }

    record Biome(
        ResourceLocation targetId,
        int xp,
        int dwellTicks,
        @Nullable String translationKey
    ) implements DiscoveryTarget {
        public Biome {
            validate(targetId, xp, dwellTicks);
        }

        @Override
        public DiscoveryType type() {
            return DiscoveryType.BIOME;
        }

        @Override
        public String candidateKey() {
            return "biome:" + targetId;
        }
    }

    record Dimension(
        ResourceLocation targetId,
        int xp,
        int dwellTicks,
        @Nullable String translationKey
    ) implements DiscoveryTarget {
        public Dimension {
            validate(targetId, xp, dwellTicks);
        }

        @Override
        public DiscoveryType type() {
            return DiscoveryType.DIMENSION;
        }

        @Override
        public String candidateKey() {
            return "dimension:" + targetId;
        }
    }

    record Structure(
        ResourceLocation targetId,
        StructureInstanceKey instanceKey,
        int xp,
        int dwellTicks,
        int maxDiscoveries,
        @Nullable String translationKey
    ) implements DiscoveryTarget {
        public Structure {
            validate(targetId, xp, dwellTicks);
            Objects.requireNonNull(instanceKey, "instance key is null");
            if (maxDiscoveries == 0 || maxDiscoveries < -1) {
                throw new IllegalArgumentException(
                    "max discoveries must be -1 or greater than 0"
                );
            }
        }

        @Override
        public DiscoveryType type() {
            return DiscoveryType.STRUCTURE;
        }

        @Override
        public String candidateKey() {
            return "structure:" + instanceKey.serialize();
        }
    }

    private static void validate(
        ResourceLocation targetId,
        int xp,
        int dwellTicks
    ) {
        Objects.requireNonNull(targetId, "target id is null");
        if (xp < 0) {
            throw new IllegalArgumentException("xp must not be negative");
        }
        if (dwellTicks < 0) {
            throw new IllegalArgumentException(
                "dwell ticks must not be negative"
            );
        }
    }
}
