package com.magu1436.craftbound.occupations.blacksmith.quality;

import com.magu1436.craftbound.common.quality.QualityState;
import java.util.List;
import java.util.Optional;

public final class BlacksmithQualityTierResolver {
    private BlacksmithQualityTierResolver() {}

    public static Optional<QualityTierDefinition> resolve(
        int quality,
        List<QualityTierDefinition> tiers
    ) {
        if (!QualityState.isValidQuality(quality)) return Optional.empty();

        return tiers.stream()
            .filter(tier -> tier.contains(quality))
            .findFirst();
    }
}
