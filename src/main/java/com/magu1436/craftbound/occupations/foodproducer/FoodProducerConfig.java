package com.magu1436.craftbound.occupations.foodproducer;

import net.minecraftforge.common.ForgeConfigSpec;

/** FoodProducerのサーバー設定。 */
public final class FoodProducerConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.BooleanValue DIET_INTEGRATION_ENABLED;
    private static final ForgeConfigSpec.DoubleValue DIET_GAIN_PER_POINT;
    private static final ForgeConfigSpec.DoubleValue PRESERVED_DIET_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue PROFESSIONAL_MEAL_DIET_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue MAXIMUM_DIET_GAIN_PER_GROUP;

    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("dietIntegration");
        DIET_INTEGRATION_ENABLED = BUILDER
                .comment("Enable Craftbound FoodProducer integration when Diet is installed.")
                .define("enabled", true);
        DIET_GAIN_PER_POINT = BUILDER
                .comment("Diet group gain for one inherited Craftbound diet point (0.04 = 4%).")
                .defineInRange("gainPerPoint", 0.04D, 0.0D, 1.0D);
        PRESERVED_DIET_MULTIPLIER = BUILDER
                .comment("Diet gain multiplier for preserved foods.")
                .defineInRange("preservedMultiplier", 1.25D, 0.0D, 10.0D);
        PROFESSIONAL_MEAL_DIET_MULTIPLIER = BUILDER
                .comment("Diet gain multiplier for professional meals.")
                .defineInRange("professionalMealMultiplier", 0.85D, 0.0D, 10.0D);
        MAXIMUM_DIET_GAIN_PER_GROUP = BUILDER
                .comment("Maximum gain applied to one Diet group by one Craftbound dish.")
                .defineInRange("maximumGainPerGroup", 0.25D, 0.0D, 1.0D);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private FoodProducerConfig() {
    }

    public static boolean isDietIntegrationEnabled() {
        return DIET_INTEGRATION_ENABLED.get();
    }

    public static double dietGainPerPoint() {
        return DIET_GAIN_PER_POINT.get();
    }

    public static double preservedDietMultiplier() {
        return PRESERVED_DIET_MULTIPLIER.get();
    }

    public static double professionalMealDietMultiplier() {
        return PROFESSIONAL_MEAL_DIET_MULTIPLIER.get();
    }

    public static double maximumDietGainPerGroup() {
        return MAXIMUM_DIET_GAIN_PER_GROUP.get();
    }
}
