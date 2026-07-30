package com.magu1436.craftbound.occupations.explorer;

/**
 * 移動による食料消耗の軽減量を計算する。
 */
public final class MovementExhaustionReductionCalculator {

    private MovementExhaustionReductionCalculator() {
    }

    public static float reduce(float exhaustion, double reduction) {
        double clampedReduction = Math.max(
            0.0D,
            Math.min(reduction, 1.0D)
        );
        return exhaustion * (float) (1.0D - clampedReduction);
    }
}
