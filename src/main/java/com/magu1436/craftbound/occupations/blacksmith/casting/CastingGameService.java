package com.magu1436.craftbound.occupations.blacksmith.casting;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import com.magu1436.craftbound.occupations.blacksmith.casting.definition.MetalPartDefinition;
import com.magu1436.craftbound.occupations.blacksmith.casting.definition.MetalPartDefinition.FailureLumpDefinition;
import com.magu1436.craftbound.occupations.blacksmith.casting.definition.MetalPartDefinitionSnapshot;
import com.magu1436.craftbound.occupations.blacksmith.casting.definition.MetalPartDefinitions;
import com.magu1436.craftbound.occupations.blacksmith.casting.lump.MetalLumpStateService;
import com.magu1436.craftbound.occupations.blacksmith.casting.part.RoughMetalPartItem;
import com.magu1436.craftbound.occupations.blacksmith.crucible.CrucibleItem;
import com.magu1436.craftbound.occupations.blacksmith.crucible.CrucibleProcessState;
import com.magu1436.craftbound.occupations.blacksmith.crucible.CrucibleState;
import com.magu1436.craftbound.occupations.blacksmith.crucible.CrucibleStateService;
import com.magu1436.craftbound.occupations.blacksmith.material.MetalDefinition;
import com.magu1436.craftbound.occupations.blacksmith.material.MetalMaterialDefinitions;
import com.magu1436.craftbound.occupations.blacksmith.material.MetalVisualData;
import com.magu1436.craftbound.occupations.blacksmith.material.MetalVisualDataService;
import com.magu1436.craftbound.occupations.blacksmith.melting.MeltingGameService;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class CastingGameService {
    private CastingGameService() {
    }

    public static CastingActionResult tryPour(
        ServerPlayer player,
        CastingTableBlockEntity table,
        ItemStack crucible
    ) {
        if (player == null
            || table == null
            || !table.canStartCasting()
            || crucible.isEmpty()
            || !(crucible.getItem() instanceof CrucibleItem)) {
            return CastingActionResult.PASS;
        }

        Optional<CrucibleState> stateResult = CrucibleStateService.read(crucible);
        if (stateResult.isEmpty()
            || stateResult.get().processState() == CrucibleProcessState.EMPTY
            || stateResult.get().metalId() == null) {
            return CastingActionResult.PASS;
        }
        CrucibleState state = stateResult.get();

        Optional<MetalPartDefinition> definitionResult =
            MetalPartDefinitions.INSTANCE.resolve(table.mold(), state.metalId());
        Optional<MetalDefinition> metalResult =
            MetalMaterialDefinitions.INSTANCE.get(state.metalId());
        OptionalInt heatingScore = MeltingGameService.evaluateHeatingScore(crucible);
        Optional<MetalVisualData> visualResult =
            MetalVisualDataService.resolve(state.metalId());
        if (definitionResult.isEmpty()
            || metalResult.isEmpty()
            || heatingScore.isEmpty()
            || visualResult.isEmpty()) {
            return CastingActionResult.PASS;
        }

        MetalPartDefinition definition = definitionResult.get();
        if (state.amount() < definition.ingredientCount()) {
            return CastingActionResult.PASS;
        }

        MetalPartDefinitionSnapshot snapshot;
        try {
            snapshot = definition.snapshot();
        } catch (RuntimeException exception) {
            return CastingActionResult.PASS;
        }
        Item roughOutput = ForgeRegistries.ITEMS.getValue(snapshot.roughOutputItemId());
        if (!(roughOutput instanceof RoughMetalPartItem)) {
            return CastingActionResult.PASS;
        }

        MetalDefinition metal = metalResult.get();
        if (state.heatingTicks() < metal.castableAfterTicks()) {
            return handleEarlyPour(table, crucible, state, definition, metal);
        }
        if (state.heatingTicks() >= metal.destroyAfterTicks()) {
            return consumeWithoutOutput(table, crucible, definition.ingredientCount());
        }

        CastingProcess process;
        try {
            process = new CastingProcess(
                CastingProcess.CURRENT_VERSION,
                UUID.randomUUID(),
                player.getUUID(),
                snapshot.definitionId(),
                snapshot.metalId(),
                snapshot.ingredientCount(),
                state.heatingTicks(),
                heatingScore.getAsInt(),
                0L,
                false,
                snapshot,
                visualResult.get()
            );
        } catch (RuntimeException exception) {
            return CastingActionResult.PASS;
        }

        table.beginTransaction();
        try {
            if (!CrucibleStateService.consumeForCasting(
                crucible,
                definition.ingredientCount()
            )) {
                return CastingActionResult.PASS;
            }
            table.setActiveProcess(process);
            return CastingActionResult.SUCCESS;
        } finally {
            table.endTransaction();
        }
    }

    private static CastingActionResult handleEarlyPour(
        CastingTableBlockEntity table,
        ItemStack crucible,
        CrucibleState state,
        MetalPartDefinition definition,
        MetalDefinition metal
    ) {
        int usedAmount = definition.ingredientCount();
        int lossAmount = roundLoss(usedAmount * metal.lumpLossRatio(), metal.lumpLossRounding());
        int recoveredAmount = usedAmount - lossAmount;
        ItemStack recovered = ItemStack.EMPTY;
        if (recoveredAmount > 0) {
            FailureLumpDefinition failure = definition.failureLump();
            if ((long) failure.count() * failure.unitsPerItem() != recoveredAmount) {
                return CastingActionResult.PASS;
            }
            Optional<ItemStack> recoveredResult = MetalLumpStateService.createSmall(
                state.metalId(),
                failure.count(),
                failure.unitsPerItem()
            );
            if (recoveredResult.isEmpty()
                || !failure.itemId().equals(
                    ForgeRegistries.ITEMS.getKey(recoveredResult.get().getItem())
                )) {
                return CastingActionResult.PASS;
            }
            recovered = recoveredResult.get();
        }

        table.beginTransaction();
        try {
            if (!CrucibleStateService.consumeForCasting(crucible, usedAmount)) {
                return CastingActionResult.PASS;
            }
            table.setPendingOutput(recovered);
            return CastingActionResult.SUCCESS;
        } finally {
            table.endTransaction();
        }
    }

    private static CastingActionResult consumeWithoutOutput(
        CastingTableBlockEntity table,
        ItemStack crucible,
        int amount
    ) {
        table.beginTransaction();
        try {
            if (!CrucibleStateService.consumeForCasting(crucible, amount)) {
                return CastingActionResult.PASS;
            }
            table.markStateChangedAndSync();
            return CastingActionResult.SUCCESS;
        } finally {
            table.endTransaction();
        }
    }

    private static int roundLoss(
        double loss,
        MetalDefinition.LumpLossRounding rounding
    ) {
        return switch (rounding) {
            case CEIL -> (int) Math.ceil(loss);
            case FLOOR -> (int) Math.floor(loss);
            case ROUND -> (int) Math.round(loss);
        };
    }
}
