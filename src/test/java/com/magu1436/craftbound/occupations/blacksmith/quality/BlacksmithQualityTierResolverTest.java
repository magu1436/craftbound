package com.magu1436.craftbound.occupations.blacksmith.quality;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BlacksmithQualityTierResolverTest {
    private static final List<QualityTierDefinition> TIERS = List.of(
        new QualityTierDefinition("low", 0, 49, "quality.low"),
        new QualityTierDefinition("high", 50, 100, "quality.high")
    );

    @Test
    void resolvesInclusiveTierBoundaries() {
        assertEquals("low", BlacksmithQualityTierResolver.resolve(0, TIERS).orElseThrow().id());
        assertEquals("low", BlacksmithQualityTierResolver.resolve(49, TIERS).orElseThrow().id());
        assertEquals("high", BlacksmithQualityTierResolver.resolve(50, TIERS).orElseThrow().id());
        assertEquals("high", BlacksmithQualityTierResolver.resolve(100, TIERS).orElseThrow().id());
    }

    @Test
    void rejectsQualityOutsideSharedRange() {
        assertTrue(BlacksmithQualityTierResolver.resolve(-1, TIERS).isEmpty());
        assertTrue(BlacksmithQualityTierResolver.resolve(101, TIERS).isEmpty());
    }
}
