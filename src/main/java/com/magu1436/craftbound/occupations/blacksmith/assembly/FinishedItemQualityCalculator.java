package com.magu1436.craftbound.occupations.blacksmith.assembly;

import com.magu1436.craftbound.common.quality.QualityState;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

public final class FinishedItemQualityCalculator {
    private FinishedItemQualityCalculator() {}

    public static OptionalInt calculate(List<Integer> qualities) {
        Objects.requireNonNull(qualities, "qualities");
        if (qualities.isEmpty()) return OptionalInt.empty();

        long total = 0L;
        for (Integer quality : qualities) {
            if (quality == null || !QualityState.isValidQuality(quality)) {
                throw new IllegalArgumentException("quality is outside the valid range: " + quality);
            }
            total += quality;
        }
        return OptionalInt.of((int) (total / qualities.size()));
    }
}
