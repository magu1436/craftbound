package com.magu1436.craftbound.occupations.blacksmith.melting;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import com.magu1436.craftbound.common.CraftboundUtilities;
import com.magu1436.craftbound.occupations.blacksmith.crucible.CrucibleProcessState;
import com.magu1436.craftbound.occupations.blacksmith.crucible.CrucibleState;
import com.magu1436.craftbound.occupations.blacksmith.crucible.CrucibleStateService;
import com.magu1436.craftbound.occupations.blacksmith.material.MetalDefinition;
import com.magu1436.craftbound.occupations.blacksmith.material.MetalDefinition.HeatingScorePoint;
import com.magu1436.craftbound.occupations.blacksmith.material.MetalMaterialDefinitions;
import com.magu1436.craftbound.occupations.blacksmith.melting.state.HeatingPhase;
import com.magu1436.craftbound.occupations.blacksmith.melting.state.HeatingStatus;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class MeltingGameService {

    private static final ResourceLocation LINEAR_CURVE =
            CraftboundUtilities.createResourceLocation("linear_curve");

    private MeltingGameService() {
    }

    public static Optional<HeatingStatus> evaluate(
        ItemStack crucible,
        long pendingHeatingTicks
    ) {
        if (pendingHeatingTicks < 0L) {
            return Optional.empty();
        }

        Optional<CrucibleState> stateResult =
            CrucibleStateService.read(crucible);
        if (stateResult.isEmpty()) {
            return Optional.empty();
        }
        CrucibleState crucibleState = stateResult.get();
        if (crucibleState.processState() == CrucibleProcessState.EMPTY) {
            return Optional.of(new HeatingStatus(HeatingPhase.UNHEATED, false));
        }

        Optional<MetalDefinition> definitionResult =
            MetalMaterialDefinitions.INSTANCE.get(crucibleState.metalId());
        if (definitionResult.isEmpty()) {
            return Optional.empty();
        }
        MetalDefinition definition = definitionResult.get();
        if (!LINEAR_CURVE.equals(definition.heatingEvaluator())) {
            return Optional.empty();
        }

        long effectiveHeatingTicks = saturatedAdd(
            crucibleState.heatingTicks(),
            pendingHeatingTicks
        );
        HeatingPhase phase = determinePhase(definition, effectiveHeatingTicks);
        return Optional.of(new HeatingStatus(
            phase,
            effectiveHeatingTicks >= definition.dangerAfterTicks()
        ));
    }

    public static boolean commitHeating(
        ItemStack crucible,
        long pendingHeatingTicks
    ) {
        if (pendingHeatingTicks < 0L) {
            return false;
        }
        if (pendingHeatingTicks == 0L) {
            return true;
        }
        return CrucibleStateService.advanceHeating(
            crucible,
            pendingHeatingTicks
        );
    }

    public static OptionalInt evaluateHeatingScore(ItemStack crucible) {
        Optional<CrucibleState> stateResult = CrucibleStateService.read(crucible);
        if (stateResult.isEmpty()) {
            return OptionalInt.empty();
        }

        CrucibleState state = stateResult.get();
        if (state.processState() == CrucibleProcessState.EMPTY) {
            return OptionalInt.empty();
        }

        Optional<MetalDefinition> definitionResult =
            MetalMaterialDefinitions.INSTANCE.get(state.metalId());
        if (definitionResult.isEmpty()) {
            return OptionalInt.empty();
        }

        MetalDefinition definition = definitionResult.get();
        if (!LINEAR_CURVE.equals(definition.heatingEvaluator())) {
            return OptionalInt.empty();
        }
        return evaluateLinearHeatingScore(
            definition.scoreCurve(),
            state.heatingTicks()
        );
    }

    static OptionalInt evaluateLinearHeatingScore(
        List<HeatingScorePoint> scoreCurve,
        long heatingTicks
    ) {
        if (scoreCurve.isEmpty() || heatingTicks < 0L) {
            return OptionalInt.empty();
        }

        HeatingScorePoint first = scoreCurve.get(0);
        if (heatingTicks <= first.ticks()) {
            return OptionalInt.of(clampScore(first.score()));
        }

        for (int index = 1; index < scoreCurve.size(); index++) {
            HeatingScorePoint left = scoreCurve.get(index - 1);
            HeatingScorePoint right = scoreCurve.get(index);
            if (right.ticks() <= left.ticks()) {
                return OptionalInt.empty();
            }
            if (heatingTicks <= right.ticks()) {
                double progress = (double) (heatingTicks - left.ticks())
                    / (double) (right.ticks() - left.ticks());
                double interpolated = left.score()
                    + (right.score() - left.score()) * progress;
                return OptionalInt.of(clampScore((int) Math.round(interpolated)));
            }
        }

        return OptionalInt.of(clampScore(
            scoreCurve.get(scoreCurve.size() - 1).score()
        ));
    }

    static HeatingPhase determinePhase(
        MetalDefinition definition,
        long effectiveHeatingTicks
    ) {
        if (effectiveHeatingTicks == 0L) {
            return HeatingPhase.UNHEATED;
        }
        if (effectiveHeatingTicks < definition.castableAfterTicks()) {
            return HeatingPhase.TOO_EARLY;
        }
        if (effectiveHeatingTicks >= definition.dangerAfterTicks()) {
            return HeatingPhase.OVERHEATED;
        }
        if (isOptimal(definition.scoreCurve(), effectiveHeatingTicks)) {
            return HeatingPhase.OPTIMAL;
        }
        return HeatingPhase.CASTABLE;
    }

    static long saturatedAdd(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }

    private static boolean isOptimal(
        List<HeatingScorePoint> scoreCurve,
        long effectiveHeatingTicks
    ) {
        HeatingScorePoint previous = null;
        for (HeatingScorePoint point : scoreCurve) {
            if (effectiveHeatingTicks == point.ticks()) {
                return point.score() == 100;
            }
            if (effectiveHeatingTicks < point.ticks()) {
                return previous != null
                    && previous.score() == 100
                    && point.score() == 100;
            }
            previous = point;
        }
        return false;
    }

    private static int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
