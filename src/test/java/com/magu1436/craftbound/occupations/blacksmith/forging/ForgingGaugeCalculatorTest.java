package com.magu1436.craftbound.occupations.blacksmith.forging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.magu1436.craftbound.occupations.blacksmith.forging.state.GaugeDirection;
import org.junit.jupiter.api.Test;

class ForgingGaugeCalculatorTest {
    @Test
    void followsDefaultTriangleWave() {
        assertValueAt(0L, 0.0D);
        assertValueAt(15L, 50.0D);
        assertValueAt(30L, 100.0D);
        assertValueAt(45L, 50.0D);
        assertValueAt(60L, 0.0D);
    }

    @Test
    void resumesFromArbitraryValueAndDirection() {
        ForgingGaugeCalculator.GaugeSnapshot snapshot = ForgingGaugeCalculator.valueAt(
            100L,
            50.0D,
            GaugeDirection.DOWN,
            115L
        );
        assertEquals(0.0D, snapshot.value(), 0.000001D);
        assertEquals(GaugeDirection.UP, snapshot.direction());
    }

    private static void assertValueAt(long tick, double expected) {
        ForgingGaugeCalculator.GaugeSnapshot snapshot = ForgingGaugeCalculator.valueAt(
            0L,
            0.0D,
            GaugeDirection.UP,
            tick
        );
        assertEquals(expected, snapshot.value(), 0.000001D);
    }
}
