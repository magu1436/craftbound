package com.magu1436.craftbound.occupations.foodproducer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.magu1436.craftbound.occupations.foodproducer.processing.FoodRoleMobEffect;

class FoodRoleMobEffectTest {

    @Test
    void encodesRecipeAmountIntoStableAmplifierSnapshot() {
        assertEquals(10, FoodRoleMobEffect.encodeAmount(0.05D));
        assertEquals(15, FoodRoleMobEffect.encodeAmount(0.075D));
        assertEquals(25, FoodRoleMobEffect.encodeAmount(0.125D));
        assertEquals(50, FoodRoleMobEffect.encodeAmount(0.25D));
        assertEquals(0.075D, FoodRoleMobEffect.decodeAmount(15), 0.000001D);

        FoodRoleMobEffect armor = new FoodRoleMobEffect(
                0,
                FoodRoleMobEffect.AmountDisplay.FLAT,
                2.0D
        );
        assertEquals(100, armor.encodeEffectiveAmount(1.0D));
        assertEquals(1.0D, armor.decodeEffectiveAmount(100), 0.000001D);
    }

    @Test
    void clampsInvalidRecipeAmountsToSupportedRange() {
        assertEquals(0, FoodRoleMobEffect.encodeAmount(-1.0D));
        assertEquals(Byte.MAX_VALUE, FoodRoleMobEffect.encodeAmount(2.0D));
    }
}
