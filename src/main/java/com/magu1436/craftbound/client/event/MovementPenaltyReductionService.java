package com.magu1436.craftbound.client.event;

import java.util.List;
import java.util.Objects;

import com.magu1436.craftbound.event.MovementPenaltyReductionRule;

import net.minecraft.client.player.Input;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.MovementInputUpdateEvent;

public final class MovementPenaltyReductionService {

    private static final double VANILLA_ITEM_USE_MULTIPLIER = 0.2D;

    private MovementPenaltyReductionService() {
    }

    // TODO: クライアント入力の補正だけでなく、サーバー側でも補正後の
    // 移動量が不正な移動にならない範囲内か検証する共通処理を追加する。
    public static void reduceMovementPenalty(
        MovementInputUpdateEvent event,
        List<MovementPenaltyReductionRule> rules
    ) {
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(rules, "rules must not be null");

        Player player = event.getEntity();
        if (!player.isUsingItem() || player.isPassenger()) {
            return;
        }

        double reductionRate = resolveReductionRate(
            player,
            player.getUseItem(),
            rules
        );
        if (reductionRate <= 0.0D) {
            return;
        }

        float compensationMultiplier = calculateCompensationMultiplier(
            VANILLA_ITEM_USE_MULTIPLIER,
            reductionRate
        );
        Input input = event.getInput();
        input.leftImpulse *= compensationMultiplier;
        input.forwardImpulse *= compensationMultiplier;
    }

    public static double calculateFinalMultiplier(
        double vanillaMultiplier,
        double reductionRate
    ) {
        validateVanillaMultiplier(vanillaMultiplier);
        if (!Double.isFinite(reductionRate)) {
            throw new IllegalArgumentException(
                "reductionRate must be a finite value"
            );
        }

        double clampedReductionRate = Mth.clamp(
            reductionRate,
            0.0D,
            1.0D
        );
        return 1.0D
            - (
                (1.0D - vanillaMultiplier)
                    * (1.0D - clampedReductionRate)
            );
    }

    public static float calculateCompensationMultiplier(
        double vanillaMultiplier,
        double reductionRate
    ) {
        return (float) (
            calculateFinalMultiplier(vanillaMultiplier, reductionRate)
                / vanillaMultiplier
        );
    }

    private static double resolveReductionRate(
        Player player,
        ItemStack itemStack,
        List<MovementPenaltyReductionRule> rules
    ) {
        double reductionRate = 0.0D;

        for (MovementPenaltyReductionRule rule : rules) {
            Objects.requireNonNull(
                rule,
                "rules must not contain null"
            );
            if (!rule.matches(itemStack)) {
                continue;
            }

            Attribute attribute = rule.attribute().get();
            if (attribute == null) {
                continue;
            }

            AttributeInstance attributeInstance = player.getAttribute(
                attribute
            );
            if (attributeInstance == null) {
                continue;
            }

            double candidate = attributeInstance.getValue()
                * rule.reductionRatePerPoint();
            if (!Double.isFinite(candidate)) {
                continue;
            }

            reductionRate = Math.max(reductionRate, candidate);
        }

        return Mth.clamp(reductionRate, 0.0D, 1.0D);
    }

    private static void validateVanillaMultiplier(
        double vanillaMultiplier
    ) {
        if (
            !Double.isFinite(vanillaMultiplier)
                || vanillaMultiplier <= 0.0D
                || vanillaMultiplier > 1.0D
        ) {
            throw new IllegalArgumentException(
                "vanillaMultiplier must be greater than 0 and at most 1"
            );
        }
    }
}
