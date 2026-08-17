package com.magu1436.craftbound.occupations.blacksmith.quality;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class BlacksmithQualityResolverTest {
    @Test
    void performanceUsesExplicitQualityForTarget() {
        assertEquals(80, BlacksmithQualityResolver.resolveForPerformance(
            true, OptionalInt.of(80), 30).orElseThrow());
    }

    @Test
    void performanceUsesDefaultOnlyForTargetWithoutState() {
        assertEquals(30, BlacksmithQualityResolver.resolveForPerformance(
            true, OptionalInt.empty(), 30).orElseThrow());
        assertTrue(BlacksmithQualityResolver.resolveForPerformance(
            false, OptionalInt.empty(), 30).isEmpty());
        assertTrue(BlacksmithQualityResolver.resolveForPerformance(
            false, OptionalInt.of(80), 30).isEmpty());
    }

    @Test
    void tooltipKeepsExplicitQualityForNonTargetIntermediateItem() {
        assertEquals(70, BlacksmithQualityResolver.resolveForTooltip(
            false, OptionalInt.of(70), 30).orElseThrow());
    }

    @Test
    void tooltipUsesDefaultForTargetAndHidesUntrackedNonTarget() {
        assertEquals(30, BlacksmithQualityResolver.resolveForTooltip(
            true, OptionalInt.empty(), 30).orElseThrow());
        assertTrue(BlacksmithQualityResolver.resolveForTooltip(
            false, OptionalInt.empty(), 30).isEmpty());
    }
}
