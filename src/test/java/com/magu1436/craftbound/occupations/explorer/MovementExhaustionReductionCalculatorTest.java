package com.magu1436.craftbound.occupations.explorer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MovementExhaustionReductionCalculatorTest {

    @Test
    void reducesExhaustionByAttributeValue() {
        assertEquals(
            0.075F,
            MovementExhaustionReductionCalculator.reduce(0.1F, 0.25D),
            0.000001F
        );
    }

    @Test
    void clampsReductionToSupportedRange() {
        assertEquals(
            0.1F,
            MovementExhaustionReductionCalculator.reduce(0.1F, -0.5D),
            0.000001F
        );
        assertEquals(
            0.0F,
            MovementExhaustionReductionCalculator.reduce(0.1F, 1.5D),
            0.000001F
        );
    }
}
