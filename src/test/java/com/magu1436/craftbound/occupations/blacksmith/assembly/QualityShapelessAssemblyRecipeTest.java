package com.magu1436.craftbound.occupations.blacksmith.assembly;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class QualityShapelessAssemblyRecipeTest {
    @Test
    void matchesRegardlessOfStackOrder() {
        int[] assignment = QualityShapelessAssemblyRecipe.findAssignment(
            2, 2, (ingredient, stack) -> ingredient != stack);

        assertArrayEquals(new int[] {1, 0}, assignment);
    }

    @Test
    void rejectsExtraOrMissingStacks() {
        assertNull(QualityShapelessAssemblyRecipe.findAssignment(
            2, 1, (ingredient, stack) -> true));
        assertNull(QualityShapelessAssemblyRecipe.findAssignment(
            2, 3, (ingredient, stack) -> true));
    }

    @Test
    void backtracksWhenIngredientConditionsOverlap() {
        int[] assignment = QualityShapelessAssemblyRecipe.findAssignment(
            2, 2, (ingredient, stack) -> ingredient == 0 || stack == 0);

        assertArrayEquals(new int[] {1, 0}, assignment);
    }
}
