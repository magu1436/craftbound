package com.magu1436.craftbound.occupations.architect;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 建築経験値のバランス設定。
 */
public final class ArchitectConfig {
    private static final ForgeConfigSpec.Builder BUILDER =
        new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue BASE_POINT_UNITS;
    private static final ForgeConfigSpec.IntValue POINT_UNITS_PER_EXPERIENCE;
    private static final ForgeConfigSpec.IntValue RECENT_WINDOW_MINUTES;
    private static final ForgeConfigSpec.IntValue LIFETIME_FULL_LIMIT;
    private static final ForgeConfigSpec.IntValue LIFETIME_HALF_LIMIT;
    private static final ForgeConfigSpec.IntValue LIFETIME_REDUCED_LIMIT;
    private static final ForgeConfigSpec.IntValue RECENT_FULL_LIMIT;
    private static final ForgeConfigSpec.IntValue RECENT_HALF_LIMIT;
    private static final ForgeConfigSpec.IntValue RECENT_REDUCED_LIMIT;
    private static final ForgeConfigSpec.IntValue FULL_RATE;
    private static final ForgeConfigSpec.IntValue HALF_RATE;
    private static final ForgeConfigSpec.IntValue REDUCED_RATE;
    private static final ForgeConfigSpec.IntValue ZERO_RATE;
    private static final ForgeConfigSpec.IntValue PROPERTY_REDUCED_RATE;
    private static final ForgeConfigSpec.DoubleValue LOW_HARDNESS_THRESHOLD;

    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("constructionExperience");
        BASE_POINT_UNITS = definePositive("basePointUnits", 100);
        POINT_UNITS_PER_EXPERIENCE = definePositive(
            "pointUnitsPerExperience",
            1_000
        );
        RECENT_WINDOW_MINUTES = definePositive(
            "recentWindowMinutes",
            30
        );

        BUILDER.push("lifetimeUsage");
        LIFETIME_FULL_LIMIT = definePositive("fullRateLimit", 256);
        LIFETIME_HALF_LIMIT = definePositive("halfRateLimit", 1_024);
        LIFETIME_REDUCED_LIMIT = definePositive(
            "reducedRateLimit",
            4_096
        );
        BUILDER.pop();

        BUILDER.push("recentUsage");
        RECENT_FULL_LIMIT = definePositive("fullRateLimit", 128);
        RECENT_HALF_LIMIT = definePositive("halfRateLimit", 512);
        RECENT_REDUCED_LIMIT = definePositive(
            "reducedRateLimit",
            1_024
        );
        BUILDER.pop();

        BUILDER.push("rates");
        FULL_RATE = defineRate("full", 10_000);
        HALF_RATE = defineRate("half", 5_000);
        REDUCED_RATE = defineRate("reduced", 2_000);
        ZERO_RATE = defineRate("zero", 0);
        PROPERTY_REDUCED_RATE = defineRate(
            "propertyReduced",
            2_500
        );
        BUILDER.pop();

        LOW_HARDNESS_THRESHOLD = BUILDER
            .comment("Hardness below this value receives the reduced rate.")
            .defineInRange("lowHardnessThreshold", 0.2D, 0.0D, 100.0D);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private ArchitectConfig() {
    }

    public static int basePointUnits() {
        return BASE_POINT_UNITS.get();
    }

    public static int pointUnitsPerExperience() {
        return POINT_UNITS_PER_EXPERIENCE.get();
    }

    public static int recentWindowMinutes() {
        return RECENT_WINDOW_MINUTES.get();
    }

    public static int lifetimeRate(int useCount) {
        return resolveRate(
            useCount,
            LIFETIME_FULL_LIMIT.get(),
            LIFETIME_HALF_LIMIT.get(),
            LIFETIME_REDUCED_LIMIT.get()
        );
    }

    public static int recentRate(int useCount) {
        return resolveRate(
            useCount,
            RECENT_FULL_LIMIT.get(),
            RECENT_HALF_LIMIT.get(),
            RECENT_REDUCED_LIMIT.get()
        );
    }

    public static int fullRate() {
        return FULL_RATE.get();
    }

    public static int propertyReducedRate() {
        return PROPERTY_REDUCED_RATE.get();
    }

    public static double lowHardnessThreshold() {
        return LOW_HARDNESS_THRESHOLD.get();
    }

    private static int resolveRate(
        int count,
        int fullLimit,
        int halfLimit,
        int reducedLimit
    ) {
        if (fullLimit >= halfLimit || halfLimit >= reducedLimit) {
            throw new IllegalStateException(
                "usage limits must be strictly increasing"
            );
        }
        if (count <= fullLimit) {
            return FULL_RATE.get();
        }
        if (count <= halfLimit) {
            return HALF_RATE.get();
        }
        if (count <= reducedLimit) {
            return REDUCED_RATE.get();
        }
        return ZERO_RATE.get();
    }

    private static ForgeConfigSpec.IntValue definePositive(
        String name,
        int defaultValue
    ) {
        return BUILDER.defineInRange(name, defaultValue, 1, Integer.MAX_VALUE);
    }

    private static ForgeConfigSpec.IntValue defineRate(
        String name,
        int defaultValue
    ) {
        return BUILDER.defineInRange(name, defaultValue, 0, 10_000);
    }
}
