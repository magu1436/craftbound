package com.magu1436.craftbound.occupations.blacksmith.quality;

import net.minecraft.resources.ResourceLocation;

public record QualityPerformanceRule(
    ResourceLocation evaluator,
    double base,
    double perQuality,
    double roundTo,
    Rounding rounding,
    boolean preserveDamageRatio
) {
    public enum Rounding {
        NONE,
        ROUND
    }
}
