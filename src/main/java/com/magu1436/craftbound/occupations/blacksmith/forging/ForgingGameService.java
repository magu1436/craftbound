package com.magu1436.craftbound.occupations.blacksmith.forging;

import com.magu1436.craftbound.common.quality.QualityStateService;
import com.magu1436.craftbound.occupations.blacksmith.casting.definition.MetalPartDefinition.FailureLumpDefinition;
import com.magu1436.craftbound.occupations.blacksmith.casting.finished.MetalPartStateService;
import com.magu1436.craftbound.occupations.blacksmith.casting.lump.MetalLumpStateService;
import com.magu1436.craftbound.occupations.blacksmith.casting.part.RoughMetalPartState;
import com.magu1436.craftbound.occupations.blacksmith.casting.part.RoughMetalPartStateService;
import com.magu1436.craftbound.occupations.blacksmith.forging.evaluation.ForgingEvaluator;
import com.magu1436.craftbound.occupations.blacksmith.forging.evaluation.MetalPartQualityEvaluator;
import com.magu1436.craftbound.occupations.blacksmith.forging.state.ForgingProgressState;
import com.magu1436.craftbound.occupations.blacksmith.forging.state.ForgingProgressStateService;
import com.magu1436.craftbound.occupations.blacksmith.forging.state.GaugeDirection;
import com.magu1436.craftbound.registry.CraftboundItems;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class ForgingGameService {
    private ForgingGameService() {}

    public static Optional<ForgingProgressState> startOrResume(
        ForgingTableBlockEntity table,
        double initialGaugeValue,
        GaugeDirection initialDirection
    ) {
        ItemStack workingPart = table.getWorkingPart();
        if (RoughMetalPartStateService.read(workingPart).isEmpty()) return Optional.empty();

        if (ForgingProgressStateService.hasStoredState(workingPart)) {
            return ForgingProgressStateService.read(workingPart);
        }
        if (!ForgingProgressStateService.initialize(
            workingPart,
            initialGaugeValue,
            initialDirection
        ) || !table.updateWorkingPart(workingPart)) {
            return Optional.empty();
        }
        return ForgingProgressStateService.read(workingPart);
    }

    public static boolean pause(
        ForgingTableBlockEntity table,
        double gaugeValue,
        GaugeDirection direction
    ) {
        ItemStack workingPart = table.getWorkingPart();
        return ForgingProgressStateService.saveGauge(workingPart, gaugeValue, direction)
            && table.updateWorkingPart(workingPart);
    }

    public static StrikeResult acceptStrike(
        ForgingTableBlockEntity table,
        ServerPlayer player,
        double strength
    ) {
        ItemStack hammer = player.getMainHandItem();
        if (!hammer.is(CraftboundItems.SMITHING_HAMMER.get())
            || !ForgingProgressState.isValidValue(strength)) {
            return StrikeResult.REJECTED;
        }

        ItemStack workingPart = table.getWorkingPart();
        Optional<RoughMetalPartState> roughResult = RoughMetalPartStateService.read(workingPart);
        Optional<ForgingProgressState> progressResult =
            ForgingProgressStateService.read(workingPart);
        if (roughResult.isEmpty() || progressResult.isEmpty()) return StrikeResult.REJECTED;

        RoughMetalPartState roughState = roughResult.get();
        int nextHitCount = progressResult.get().strikeHistory().size() + 1;
        boolean terminalStrike = nextHitCount >= roughState.effectiveBreakOnHit();
        boolean progressSaved = terminalStrike
            ? ForgingProgressStateService.saveTerminalStrike(workingPart, strength)
            : ForgingProgressStateService.appendStrike(workingPart, strength).isPresent();
        if (!progressSaved || !table.updateWorkingPart(workingPart)) return StrikeResult.ERROR;

        ForgingHammerDurabilityHook.onAcceptedStrike(player, hammer);
        if (!terminalStrike) return StrikeResult.ACCEPTED;
        return fail(table, player, roughState) ? StrikeResult.FAILED : StrikeResult.ERROR;
    }

    public static CompletionResult complete(
        ForgingTableBlockEntity table,
        ServerPlayer player
    ) {
        if (!table.tryReserveCompletion()) return CompletionResult.REJECTED;

        ItemStack workingPart = table.getWorkingPart();
        Optional<RoughMetalPartState> roughResult = RoughMetalPartStateService.read(workingPart);
        if (roughResult.isEmpty()) return cancel(table, CompletionResult.ERROR);
        RoughMetalPartState roughState = roughResult.get();

        List<Double> strikes;
        if (ForgingProgressStateService.hasStoredState(workingPart)) {
            Optional<ForgingProgressState> progress = ForgingProgressStateService.read(workingPart);
            if (progress.isEmpty()) return cancel(table, CompletionResult.ERROR);
            strikes = progress.get().strikeHistory();
        } else {
            strikes = List.of();
        }

        OptionalInt forgingScore = ForgingEvaluator.evaluate(
            strikes,
            roughState.definitionSnapshot().forging()
        );
        if (forgingScore.isEmpty()) return cancel(table, CompletionResult.ERROR);
        OptionalInt finalQuality = MetalPartQualityEvaluator.evaluate(
            roughState.heatingScore(),
            forgingScore.getAsInt(),
            roughState.definitionSnapshot().partQuality()
        );
        if (finalQuality.isEmpty()) return cancel(table, CompletionResult.ERROR);

        Optional<ItemStack> outputResult = MetalPartStateService.createFromRoughPart(workingPart);
        if (outputResult.isEmpty()) return cancel(table, CompletionResult.ERROR);
        ItemStack output = outputResult.get();
        if (!QualityStateService.setQuality(output, finalQuality.getAsInt())) {
            return cancel(table, CompletionResult.ERROR);
        }
        if (!table.commitReservedResult(player, output)) {
            return cancel(table, CompletionResult.ERROR);
        }

        ForgingExperienceHook.onResult(player, experienceResult(player, roughState, true, false));
        return CompletionResult.COMPLETED;
    }

    public static boolean fail(ForgingTableBlockEntity table, ServerPlayer player) {
        Optional<RoughMetalPartState> roughState =
            RoughMetalPartStateService.read(table.getWorkingPart());
        return roughState.isPresent() && fail(table, player, roughState.get());
    }

    public static boolean recoverPendingOutputs(
        ForgingTableBlockEntity table,
        ServerPlayer player
    ) {
        return table.tryCollectPendingOutputs(player);
    }

    private static boolean fail(
        ForgingTableBlockEntity table,
        ServerPlayer player,
        RoughMetalPartState roughState
    ) {
        if (!table.tryReserveCompletion()) return false;

        FailureLumpDefinition failure = roughState.definitionSnapshot().failureLump();
        Optional<ItemStack> output = MetalLumpStateService.create(
            failure.itemId(),
            roughState.metalId(),
            failure.count(),
            failure.unitsPerItem()
        );
        if (output.isEmpty() || !table.commitReservedResult(player, output.get())) {
            table.cancelCompletionReservation();
            return false;
        }

        ForgingExperienceHook.onResult(player, experienceResult(player, roughState, false, true));
        return true;
    }

    private static ForgingExperienceResult experienceResult(
        ServerPlayer player,
        RoughMetalPartState state,
        boolean success,
        boolean permanentMaterialLoss
    ) {
        return new ForgingExperienceResult(
            UUID.randomUUID(),
            player.getUUID(),
            state.ingredientCount(),
            success,
            permanentMaterialLoss
        );
    }

    private static CompletionResult cancel(
        ForgingTableBlockEntity table,
        CompletionResult result
    ) {
        table.cancelCompletionReservation();
        return result;
    }

    public enum StrikeResult {
        REJECTED,
        ACCEPTED,
        FAILED,
        ERROR
    }

    public enum CompletionResult {
        REJECTED,
        COMPLETED,
        ERROR
    }
}
