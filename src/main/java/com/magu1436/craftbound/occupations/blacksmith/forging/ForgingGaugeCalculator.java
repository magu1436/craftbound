package com.magu1436.craftbound.occupations.blacksmith.forging;

import com.magu1436.craftbound.occupations.blacksmith.forging.state.GaugeDirection;

public final class ForgingGaugeCalculator {
    public static final double DEFAULT_MIN = 0.0D;
    public static final double DEFAULT_MAX = 100.0D;
    public static final int DEFAULT_CYCLE_TICKS = 60;

    private ForgingGaugeCalculator() {}

    public static GaugeSnapshot valueAt(
        long baseServerTick,
        double baseGaugeValue,
        GaugeDirection baseDirection,
        long targetServerTick
    ) {
        return valueAt(
            baseServerTick,
            baseGaugeValue,
            baseDirection,
            targetServerTick,
            DEFAULT_MIN,
            DEFAULT_MAX,
            DEFAULT_CYCLE_TICKS
        );
    }

    public static GaugeSnapshot valueAt(
        long baseServerTick,
        double baseGaugeValue,
        GaugeDirection baseDirection,
        long targetServerTick,
        double min,
        double max,
        int cycleTicks
    ) {
        validateDefinition(baseGaugeValue, baseDirection, min, max, cycleTicks);
        double halfCycle = cycleTicks / 2.0D;
        double normalized = (baseGaugeValue - min) / (max - min);
        double basePhase = baseDirection == GaugeDirection.UP
            ? normalized * halfCycle
            : cycleTicks - normalized * halfCycle;
        double elapsed = (double) targetServerTick - (double) baseServerTick;
        double phase = positiveModulo(basePhase + elapsed, cycleTicks);

        boolean ascending = phase < halfCycle;
        double phaseValue = ascending ? phase : cycleTicks - phase;
        double value = min + (phaseValue / halfCycle) * (max - min);
        GaugeDirection direction = ascending ? GaugeDirection.UP : GaugeDirection.DOWN;
        return new GaugeSnapshot(value, direction);
    }

    private static void validateDefinition(
        double baseGaugeValue,
        GaugeDirection baseDirection,
        double min,
        double max,
        int cycleTicks
    ) {
        if (baseDirection == null
            || !Double.isFinite(min)
            || !Double.isFinite(max)
            || min >= max
            || cycleTicks < 2
            || !Double.isFinite(baseGaugeValue)
            || baseGaugeValue < min
            || baseGaugeValue > max) {
            throw new IllegalArgumentException("invalid forging gauge definition or base state");
        }
    }

    private static double positiveModulo(double value, double modulus) {
        double result = value % modulus;
        return result < 0.0D ? result + modulus : result;
    }

    public record GaugeSnapshot(double value, GaugeDirection direction) {}
}
