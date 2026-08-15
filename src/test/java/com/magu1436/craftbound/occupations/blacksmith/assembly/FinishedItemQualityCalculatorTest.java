package com.magu1436.craftbound.occupations.blacksmith.assembly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class FinishedItemQualityCalculatorTest {
    @Test
    void floorsArithmeticMean() {
        assertEquals(82, FinishedItemQualityCalculator.calculate(List.of(80, 85)).orElseThrow());
    }

    @Test
    void returnsEmptyWhenNoPartContributes() {
        assertTrue(FinishedItemQualityCalculator.calculate(List.of()).isEmpty());
    }

    @Test
    void rejectsQualityOutsideSharedRange() {
        assertThrows(IllegalArgumentException.class,
            () -> FinishedItemQualityCalculator.calculate(List.of(101)));
    }
}
