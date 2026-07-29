package com.magu1436.craftbound.occupations.adventurer;

import java.util.List;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 冒険家のサーバー設定。
 */
public final class AdventurerConfig {
    private static final ForgeConfigSpec.Builder BUILDER =
        new ForgeConfigSpec.Builder();

    private static final EmergencyEvasionConfigValues EMERGENCY_EVASION =
        defineEmergencyEvasionConfig();

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private AdventurerConfig() {
    }

    public static double getEmergencyEvasionHorizontalSpeed(int stage) {
        return EMERGENCY_EVASION
            .horizontalSpeeds()
            .get(getEmergencyEvasionStageIndex(stage))
            .get();
    }

    public static int getEmergencyEvasionCooldownTicks(int stage) {
        return EMERGENCY_EVASION
            .cooldownTicks()
            .get(getEmergencyEvasionStageIndex(stage))
            .get();
    }

    private static EmergencyEvasionConfigValues defineEmergencyEvasionConfig() {
        BUILDER.push("emergencyEvasion");

        List<ForgeConfigSpec.DoubleValue> horizontalSpeeds = List.of(
            BUILDER
                .comment("Horizontal speed for Emergency Evasion I.")
                .defineInRange(
                    "stage1HorizontalSpeed",
                    0.55D,
                    0.0D,
                    4.0D
                ),
            BUILDER
                .comment("Horizontal speed for Emergency Evasion II.")
                .defineInRange(
                    "stage2HorizontalSpeed",
                    0.65D,
                    0.0D,
                    4.0D
                ),
            BUILDER
                .comment("Horizontal speed for Emergency Evasion III.")
                .defineInRange(
                    "stage3HorizontalSpeed",
                    0.75D,
                    0.0D,
                    4.0D
                ),
            BUILDER
                .comment("Horizontal speed for Emergency Evasion IV.")
                .defineInRange(
                    "stage4HorizontalSpeed",
                    0.90D,
                    0.0D,
                    4.0D
                )
        );

        List<ForgeConfigSpec.IntValue> cooldownTicks = List.of(
            BUILDER
                .comment("Cooldown ticks for Emergency Evasion I.")
                .defineInRange(
                    "stage1CooldownTicks",
                    320,
                    0,
                    72000
                ),
            BUILDER
                .comment("Cooldown ticks for Emergency Evasion II.")
                .defineInRange(
                    "stage2CooldownTicks",
                    280,
                    0,
                    72000
                ),
            BUILDER
                .comment("Cooldown ticks for Emergency Evasion III.")
                .defineInRange(
                    "stage3CooldownTicks",
                    240,
                    0,
                    72000
                ),
            BUILDER
                .comment("Cooldown ticks for Emergency Evasion IV.")
                .defineInRange(
                    "stage4CooldownTicks",
                    200,
                    0,
                    72000
                )
        );

        BUILDER.pop();

        return new EmergencyEvasionConfigValues(
            horizontalSpeeds,
            cooldownTicks
        );
    }

    private static int getEmergencyEvasionStageIndex(int stage) {
        if (stage < 1 || stage > 4) {
            throw new IllegalArgumentException(
                "Emergency Evasion stage must be between 1 and 4"
            );
        }

        return stage - 1;
    }

    private record EmergencyEvasionConfigValues(
        List<ForgeConfigSpec.DoubleValue> horizontalSpeeds,
        List<ForgeConfigSpec.IntValue> cooldownTicks
    ) {
    }
}
