package com.magu1436.craftbound.occupations.blacksmith.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class BlacksmithQualityPerformanceEventsTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void distributesAdjustedTotalProportionally() {
        List<Double> adjusted = BlacksmithQualityPerformanceEvents.distributeProportionally(
            List.of(2.0D, 3.0D, 5.0D),
            13.0D
        );

        assertEquals(2.6D, adjusted.get(0), EPSILON);
        assertEquals(3.9D, adjusted.get(1), EPSILON);
        assertEquals(6.5D, adjusted.get(2), EPSILON);
        assertEquals(13.0D, adjusted.stream().mapToDouble(Double::doubleValue).sum(), EPSILON);
    }

    @Test
    void assignsFloatingPointResidualToLastModifier() {
        List<Double> adjusted = BlacksmithQualityPerformanceEvents.distributeProportionally(
            List.of(1.0D, 1.0D, 1.0D),
            1.0D
        );

        assertEquals(1.0D, adjusted.stream().mapToDouble(Double::doubleValue).sum(), 0.0D);
    }

    @Test
    void leavesZeroTotalUnchanged() {
        List<Double> original = List.of(-1.0D, 1.0D);

        assertEquals(original, BlacksmithQualityPerformanceEvents.distributeProportionally(original, 5.0D));
    }
}
