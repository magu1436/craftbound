package com.magu1436.craftbound.occupations.foodproducer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.magu1436.craftbound.occupations.foodproducer.processing.FoodDietValues;
import com.magu1436.craftbound.occupations.foodproducer.processing.ProfessionalMealPolicy;

class FoodProducerDietLogicTest {

    @Test
    void inheritsIngredientCategoriesByConsumedAndProducedCounts() {
        Map<String, Float> values = FoodDietValues.inheritedValues(List.of(
                new FoodDietValues.Portion(Map.of(FoodDietValues.GRAINS, 1.0F), 2),
                new FoodDietValues.Portion(Map.of(FoodDietValues.VEGETABLES, 1.0F), 1)
        ), 2);

        assertEquals(1.0F, values.get(FoodDietValues.GRAINS));
        assertEquals(0.5F, values.get(FoodDietValues.VEGETABLES));
        assertFalse(values.containsKey(FoodDietValues.FRUITS));
    }

    @Test
    void clampsInheritedCategoriesAndIgnoresUnknownValues() {
        Map<String, Float> values = FoodDietValues.inheritedValues(List.of(
                new FoodDietValues.Portion(Map.of(
                        FoodDietValues.GRAINS, 9.0F,
                        FoodDietValues.PROTEINS, -1.0F,
                        "unknown", 4.0F
                ), 1)
        ), 1);

        assertEquals(Map.of(FoodDietValues.GRAINS, FoodDietValues.MAX_VALUE), values);
    }

    @Test
    void appliesConfiguredMealMultipliersAndPerGroupMaximum() {
        assertEquals(0.04F, FoodDietValues.scaledGain(1.0F, 0.04D, 1.0D, 0.25D), 0.000001F);
        assertEquals(0.05F, FoodDietValues.scaledGain(1.0F, 0.04D, 1.25D, 0.25D), 0.000001F);
        assertEquals(0.034F, FoodDietValues.scaledGain(1.0F, 0.04D, 0.85D, 0.25D), 0.000001F);
        assertEquals(0.25F, FoodDietValues.scaledGain(10.0F, 0.04D, 1.25D, 0.25D), 0.000001F);
        assertEquals(0.0F, FoodDietValues.scaledGain(0.0F, 0.04D, 1.0D, 0.25D), 0.000001F);
    }

    @Test
    void blocksOnlyDifferentProfessionalMealWhileMarkerIsActive() {
        assertTrue(ProfessionalMealPolicy.isProfessionalMeal(false, true));
        assertFalse(ProfessionalMealPolicy.isProfessionalMeal(true, true));
        assertFalse(ProfessionalMealPolicy.isProfessionalMeal(false, false));

        assertFalse(ProfessionalMealPolicy.blocksCandidate(true, "craftbound:first", "craftbound:first"));
        assertTrue(ProfessionalMealPolicy.blocksCandidate(true, "craftbound:first", "craftbound:second"));
        assertFalse(ProfessionalMealPolicy.blocksCandidate(false, "craftbound:first", "craftbound:second"));
        assertTrue(ProfessionalMealPolicy.blocksCandidate(true, "", "craftbound:second"));
    }
}
