package com.magu1436.craftbound.occupations.blacksmith.quality;

import com.magu1436.craftbound.common.quality.QualityState;
import com.magu1436.craftbound.occupations.blacksmith.data.BlacksmithQualityPerformanceDefinitions;

public final class QualityPerformanceService {
    private QualityPerformanceService() {}

    public static double multiplier(QualityPerformanceType type, int quality) {
        validateQuality(quality);
        QualityPerformanceRule rule = BlacksmithQualityPerformanceDefinitions.rule(type);
        return rule.base() + quality * rule.perQuality();
    }

    public static double apply(QualityPerformanceType type, double original, int quality) {
        double adjusted = original * multiplier(type, quality);
        double roundTo = BlacksmithQualityPerformanceDefinitions.rule(type).roundTo();
        return roundTo > 0.0D ? Math.round(adjusted / roundTo) * roundTo : adjusted;
    }

    public static float apply(QualityPerformanceType type, float original, int quality) {
        return (float) apply(type, (double) original, quality);
    }

    public static int applyMaxDurability(int original, int quality) {
        QualityPerformanceRule rule = BlacksmithQualityPerformanceDefinitions.rule(
            QualityPerformanceType.MAX_DURABILITY
        );
        double adjusted = original * multiplier(QualityPerformanceType.MAX_DURABILITY, quality);
        if (rule.rounding() != QualityPerformanceRule.Rounding.ROUND) {
            throw new IllegalStateException("max durability rule must use integer rounding");
        }
        return (int) Math.round(adjusted);
    }

    private static void validateQuality(int quality) {
        if (!QualityState.isValidQuality(quality)) {
            throw new IllegalArgumentException("quality is outside the valid range: " + quality);
        }
    }
}
