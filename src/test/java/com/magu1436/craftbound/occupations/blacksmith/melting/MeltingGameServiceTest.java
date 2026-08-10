package com.magu1436.craftbound.occupations.blacksmith.melting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.magu1436.craftbound.common.CraftboundUtilities;
import com.magu1436.craftbound.occupations.blacksmith.material.MetalDefinition;
import com.magu1436.craftbound.occupations.blacksmith.material.MetalDefinition.HeatingScorePoint;
import com.magu1436.craftbound.occupations.blacksmith.melting.state.HeatingPhase;

class MeltingGameServiceTest {

    private static final MetalDefinition DEFINITION = new MetalDefinition(
        CraftboundUtilities.createResourceLocation("test"),
        0.5D,
        MetalDefinition.LumpLossRounding.CEIL,
        200L,
        List.of(
            new HeatingScorePoint(200L, 40),
            new HeatingScorePoint(300L, 100),
            new HeatingScorePoint(360L, 100),
            new HeatingScorePoint(480L, 40)
        ),
        480L,
        560L,
        CraftboundUtilities.createResourceLocation("linear_curve"),
        0
    );

    @Test
    void determinesEveryHeatingPhaseAtItsBoundary() {
        assertEquals(
            HeatingPhase.UNHEATED,
            MeltingGameService.determinePhase(DEFINITION, 0L)
        );
        assertEquals(
            HeatingPhase.TOO_EARLY,
            MeltingGameService.determinePhase(DEFINITION, 199L)
        );
        assertEquals(
            HeatingPhase.CASTABLE,
            MeltingGameService.determinePhase(DEFINITION, 200L)
        );
        assertEquals(
            HeatingPhase.OPTIMAL,
            MeltingGameService.determinePhase(DEFINITION, 300L)
        );
        assertEquals(
            HeatingPhase.OPTIMAL,
            MeltingGameService.determinePhase(DEFINITION, 330L)
        );
        assertEquals(
            HeatingPhase.CASTABLE,
            MeltingGameService.determinePhase(DEFINITION, 361L)
        );
        assertEquals(
            HeatingPhase.OVERHEATED,
            MeltingGameService.determinePhase(DEFINITION, 480L)
        );
    }

    @Test
    void saturatedAddDoesNotWrapToNegative() {
        assertEquals(
            Long.MAX_VALUE,
            MeltingGameService.saturatedAdd(Long.MAX_VALUE, 1L)
        );
    }
}
