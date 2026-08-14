package com.magu1436.craftbound.occupations.blacksmith.forging.state;

import java.util.List;
import java.util.Objects;

public record ForgingProgressState(
    int version,
    List<Double> strikeHistory,
    double gaugeValue,
    GaugeDirection gaugeDirection
) {
    public static final int CURRENT_VERSION = 1;
    public static final double MIN_GAUGE_VALUE = 0.0D;
    public static final double MAX_GAUGE_VALUE = 100.0D;

    public ForgingProgressState {
        if (version != CURRENT_VERSION) {
            throw new IllegalArgumentException("unsupported forging progress version: " + version);
        }
        Objects.requireNonNull(strikeHistory, "strikeHistory");
        Objects.requireNonNull(gaugeDirection, "gaugeDirection");
        strikeHistory = List.copyOf(strikeHistory);
        if (!isValidValue(gaugeValue)) {
            throw new IllegalArgumentException("invalid forging gauge value: " + gaugeValue);
        }
        for (Double strength : strikeHistory) {
            if (strength == null || !isValidValue(strength)) {
                throw new IllegalArgumentException("invalid forging strike strength: " + strength);
            }
        }
    }

    public static boolean isValidValue(double value) {
        return Double.isFinite(value)
            && value >= MIN_GAUGE_VALUE
            && value <= MAX_GAUGE_VALUE;
    }
}
