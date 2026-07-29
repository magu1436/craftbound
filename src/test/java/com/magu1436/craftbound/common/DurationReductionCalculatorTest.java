package com.magu1436.craftbound.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DurationReductionCalculatorTest {

    private static final double MAX_REDUCTION_RATE = 0.4D;

    @Test
    void reducesDurationBySpecifiedRate() {
        assertEquals(
            90,
            DurationReductionCalculator.reduce(
                100,
                0.1D,
                MAX_REDUCTION_RATE
            )
        );
    }

    @Test
    void roundsFractionalDurationUp() {
        assertEquals(
            91,
            DurationReductionCalculator.reduce(
                101,
                0.1D,
                MAX_REDUCTION_RATE
            )
        );
    }

    @Test
    void clampsReductionRateToMaximum() {
        assertEquals(
            60,
            DurationReductionCalculator.reduce(
                100,
                0.8D,
                MAX_REDUCTION_RATE
            )
        );
    }

    @Test
    void treatsNegativeReductionRateAsZero() {
        assertEquals(
            100,
            DurationReductionCalculator.reduce(
                100,
                -0.1D,
                MAX_REDUCTION_RATE
            )
        );
    }

    @Test
    void keepsAtLeastOneTick() {
        assertEquals(
            1,
            DurationReductionCalculator.reduce(
                1,
                MAX_REDUCTION_RATE,
                MAX_REDUCTION_RATE
            )
        );
    }

    @Test
    void rejectsNonPositiveDuration() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DurationReductionCalculator.reduce(
                0,
                0.1D,
                MAX_REDUCTION_RATE
            )
        );
    }

    @Test
    void rejectsInvalidMaximumReductionRate() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DurationReductionCalculator.reduce(
                100,
                0.1D,
                1.1D
            )
        );
    }
}
