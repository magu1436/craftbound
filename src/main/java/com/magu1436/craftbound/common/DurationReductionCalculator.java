package com.magu1436.craftbound.common;

/**
 * 持続時間を割合で短縮する計算を提供する。
 */
public final class DurationReductionCalculator {

    private DurationReductionCalculator() {
    }

    /**
     * 指定された持続時間を短縮し、最低1tickを保証する。
     *
     * @param originalDuration 元の持続時間
     * @param reductionRate 短縮率
     * @param maxReductionRate 短縮率の上限
     * @return 短縮後の持続時間
     */
    public static int reduce(
        int originalDuration,
        double reductionRate,
        double maxReductionRate
    ) {
        if (originalDuration <= 0) {
            throw new IllegalArgumentException(
                "original duration must be greater than 0"
            );
        }
        if (
            !Double.isFinite(reductionRate)
                || !Double.isFinite(maxReductionRate)
        ) {
            throw new IllegalArgumentException(
                "reduction rates must be finite"
            );
        }
        if (maxReductionRate < 0.0D || maxReductionRate > 1.0D) {
            throw new IllegalArgumentException(
                "max reduction rate must be between 0 and 1"
            );
        }

        double clampedReductionRate = Math.max(
            0.0D,
            Math.min(reductionRate, maxReductionRate)
        );

        return Math.max(
            1,
            (int) Math.ceil(
                originalDuration * (1.0D - clampedReductionRate)
            )
        );
    }
}
