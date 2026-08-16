package com.magu1436.craftbound.occupations.blacksmith.client;

import com.magu1436.craftbound.occupations.blacksmith.quality.BlacksmithQualityTierResolver;
import com.magu1436.craftbound.occupations.blacksmith.quality.QualityTierDefinition;
import java.util.List;
import java.util.Optional;

public final class ClientBlacksmithQualityTierDefinitions {
    private static volatile List<QualityTierDefinition> current = List.of();

    private ClientBlacksmithQualityTierDefinitions() {}

    public static void replace(List<QualityTierDefinition> tiers) {
        current = List.copyOf(tiers);
    }

    public static List<QualityTierDefinition> all() {
        return current;
    }

    public static Optional<QualityTierDefinition> resolve(int quality) {
        return BlacksmithQualityTierResolver.resolve(quality, current);
    }
}
