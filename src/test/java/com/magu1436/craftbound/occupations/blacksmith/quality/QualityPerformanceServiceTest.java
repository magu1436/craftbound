package com.magu1436.craftbound.occupations.blacksmith.quality;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class QualityPerformanceServiceTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void calculatesSpecifiedLinearMultipliers() {
        assertEquals(0.70D, QualityPerformanceService.multiplier(QualityPerformanceType.ATTACK_DAMAGE, 0), EPSILON);
        assertEquals(0.88D, QualityPerformanceService.multiplier(QualityPerformanceType.ATTACK_DAMAGE, 30), EPSILON);
        assertEquals(1.00D, QualityPerformanceService.multiplier(QualityPerformanceType.ATTACK_DAMAGE, 50), EPSILON);
        assertEquals(1.18D, QualityPerformanceService.multiplier(QualityPerformanceType.ATTACK_DAMAGE, 80), EPSILON);
        assertEquals(1.30D, QualityPerformanceService.multiplier(QualityPerformanceType.ATTACK_DAMAGE, 100), EPSILON);
    }

    @Test
    void roundsArmorAndToughnessToConfiguredIncrement() {
        assertEquals(2.5D, QualityPerformanceService.apply(QualityPerformanceType.ARMOR, 3.0D, 30), EPSILON);
        assertEquals(3.5D, QualityPerformanceService.apply(QualityPerformanceType.ARMOR_TOUGHNESS, 3.0D, 80), EPSILON);
    }

    @Test
    void roundsMaximumDurabilityToInteger() {
        assertEquals(88, QualityPerformanceService.applyMaxDurability(100, 30));
        assertEquals(119, QualityPerformanceService.applyMaxDurability(101, 80));
    }

    @Test
    void rejectsQualityOutsideSharedRange() {
        assertThrows(IllegalArgumentException.class,
            () -> QualityPerformanceService.multiplier(QualityPerformanceType.MINING_SPEED, -1));
        assertThrows(IllegalArgumentException.class,
            () -> QualityPerformanceService.multiplier(QualityPerformanceType.MINING_SPEED, 101));
    }
}
