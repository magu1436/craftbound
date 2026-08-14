package com.magu1436.craftbound.occupations.blacksmith.forging.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.magu1436.craftbound.occupations.blacksmith.casting.definition.MetalPartDefinition.ForgingDefinition;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class ForgingEvaluatorTest {
    private static final ForgingDefinition DEFINITION = new ForgingDefinition(
        55.0D,
        65.0D,
        5.0D,
        6,
        20.0D,
        10,
        0.8D,
        0.2D,
        ForgingEvaluator.WEIGHTED_FORGING_ID
    );

    @Test
    void matchesSpecifiedRepresentativeScores() {
        assertScore(Collections.nCopies(6, 60.0D), 100);
        assertScore(Collections.nCopies(5, 60.0D), 96);
        assertScore(Collections.nCopies(6, 50.0D), 80);
        assertScore(List.of(), 0);
    }

    private static void assertScore(List<Double> strikes, int expected) {
        assertEquals(expected, ForgingEvaluator.evaluate(strikes, DEFINITION).orElseThrow());
    }
}
