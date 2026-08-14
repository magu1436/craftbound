package com.magu1436.craftbound.occupations.blacksmith.forging.evaluation;

import com.magu1436.craftbound.common.CraftboundUtilities;
import com.magu1436.craftbound.occupations.blacksmith.casting.definition.MetalPartDefinition.ForgingDefinition;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.resources.ResourceLocation;

public final class ForgingEvaluator {
    public static final ResourceLocation WEIGHTED_FORGING_ID =
        CraftboundUtilities.createResourceLocation("weighted_forging");

    private ForgingEvaluator() {}

    public static OptionalInt evaluate(List<Double> strikeHistory, ForgingDefinition definition) {
        if (strikeHistory == null || definition == null
            || !WEIGHTED_FORGING_ID.equals(definition.evaluatorId())
            || !isValidDefinition(definition)) {
            return OptionalInt.empty();
        }

        double strengthScore = 0.0D;
        if (!strikeHistory.isEmpty()) {
            double total = 0.0D;
            for (Double strength : strikeHistory) {
                if (strength == null || !Double.isFinite(strength)
                    || strength < 0.0D || strength > 100.0D) {
                    return OptionalInt.empty();
                }
                double distance = strength < definition.strengthMin()
                    ? definition.strengthMin() - strength
                    : Math.max(0.0D, strength - definition.strengthMax());
                total += clamp(100.0D - distance * definition.strengthPenaltyPerPoint());
            }
            strengthScore = total / strikeHistory.size();
        }

        double hitCountScore = clamp(
            100.0D - Math.abs(strikeHistory.size() - definition.idealHits())
                * definition.hitCountPenalty()
        );
        long rounded = Math.round(
            strengthScore * definition.strengthWeight()
                + hitCountScore * definition.hitCountWeight()
        );
        return OptionalInt.of((int) Math.max(0L, Math.min(100L, rounded)));
    }

    private static boolean isValidDefinition(ForgingDefinition definition) {
        return Double.isFinite(definition.strengthMin())
            && Double.isFinite(definition.strengthMax())
            && definition.strengthMin() >= 0.0D
            && definition.strengthMin() <= definition.strengthMax()
            && definition.strengthMax() <= 100.0D
            && Double.isFinite(definition.strengthPenaltyPerPoint())
            && definition.strengthPenaltyPerPoint() >= 0.0D
            && definition.idealHits() >= 0
            && Double.isFinite(definition.hitCountPenalty())
            && definition.hitCountPenalty() >= 0.0D
            && Double.isFinite(definition.strengthWeight())
            && definition.strengthWeight() >= 0.0D
            && Double.isFinite(definition.hitCountWeight())
            && definition.hitCountWeight() >= 0.0D;
    }

    private static double clamp(double value) {
        return Math.max(0.0D, Math.min(100.0D, value));
    }
}
