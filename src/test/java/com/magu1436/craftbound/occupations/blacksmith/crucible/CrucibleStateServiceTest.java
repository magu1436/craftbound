package com.magu1436.craftbound.occupations.blacksmith.crucible;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.magu1436.craftbound.occupations.blacksmith.material.ResolvedMetalMaterial;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

class CrucibleStateServiceTest {
    private static final ResourceLocation IRON = id("craftbound:iron");
    private static final ResourceLocation GOLD = id("craftbound:gold");

    @Test
    void insertingIntoEmptyCrucibleCreatesUnheatedState() {
        CrucibleInsertResult result = CrucibleStateService.calculateInsertion(
            CrucibleState.empty(),
            material(IRON, 1),
            4,
            4
        );

        assertTrue(result.succeeded());
        assertEquals(4, result.acceptedItemCount());
        assertEquals(4, result.state().amount());
        assertEquals(CrucibleProcessState.UNHEATED, result.state().processState());
    }

    @Test
    void insertionAcceptsOnlyWholeItemsThatFit() {
        CrucibleInsertResult result = CrucibleStateService.calculateInsertion(
            CrucibleState.empty(),
            material(IRON, 9),
            10,
            10
        );

        assertEquals(1, result.acceptedItemCount());
        assertEquals(9, result.addedAmount());
    }

    @Test
    void differentMetalIsRejectedWithoutStateChange() {
        CrucibleState current = state(IRON, 5, 0L);

        CrucibleInsertResult result = CrucibleStateService.calculateInsertion(
            current,
            material(GOLD, 1),
            1,
            1
        );

        assertEquals(CrucibleInsertFailure.DIFFERENT_METAL, result.failure());
        assertEquals(current, result.state());
    }

    @Test
    void insertionIntoHeatedCruciblePreservesHeatingState() {
        CrucibleState current = state(IRON, 5, 40L);

        CrucibleInsertResult result = CrucibleStateService.calculateInsertion(
            current,
            material(IRON, 1),
            2,
            2
        );

        assertEquals(7, result.state().amount());
        assertEquals(40L, result.state().heatingTicks());
        assertEquals(CrucibleProcessState.HEATED, result.state().processState());
    }

    @Test
    void heatingMarksStateAsHeatedAndAccumulatesTicks() {
        CrucibleState heated = CrucibleStateService.advanceState(
            state(IRON, 5, 0L),
            20L
        ).orElseThrow();

        assertEquals(5, heated.amount());
        assertEquals(20L, heated.heatingTicks());
        assertEquals(CrucibleProcessState.HEATED, heated.processState());
    }

    @Test
    void partialConsumptionPreservesProcessingState() {
        CrucibleState current = state(IRON, 5, 40L);

        CrucibleState remaining = CrucibleStateService.consumeState(current, 2)
            .orElseThrow();

        assertEquals(3, remaining.amount());
        assertEquals(40L, remaining.heatingTicks());
        assertEquals(CrucibleProcessState.HEATED, remaining.processState());
    }

    @Test
    void fullConsumptionReturnsNormalizedEmptyState() {
        CrucibleState result = CrucibleStateService.consumeState(
            state(IRON, 2, 40L),
            2
        ).orElseThrow();

        assertEquals(CrucibleState.empty(), result);
    }

    @Test
    void insufficientConsumptionDoesNothing() {
        assertTrue(
            CrucibleStateService.consumeState(state(IRON, 2, 40L), 3)
                .isEmpty()
        );
    }

    private static CrucibleState state(
        ResourceLocation metal,
        int amount,
        long heatingTicks
    ) {
        return new CrucibleState(
            CrucibleState.CURRENT_VERSION,
            metal,
            amount,
            heatingTicks,
            heatingTicks == 0L
                ? CrucibleProcessState.UNHEATED
                : CrucibleProcessState.HEATED
        );
    }

    private static ResolvedMetalMaterial material(
        ResourceLocation metal,
        int amountPerItem
    ) {
        return new ResolvedMetalMaterial(metal, amountPerItem);
    }

    private static ResourceLocation id(String value) {
        return ResourceLocation.tryParse(value);
    }
}
