package com.magu1436.craftbound.occupations.foodproducer.quality;

import net.minecraft.util.RandomSource;

/** 仕様書3.3の初期品質確率表. */
public final class FoodQualityRolls {

    private static final int[] HIGH_PERCENT = {5, 10, 15, 25, 35, 50};
    private static final int[] STANDARD_PERCENT = {35, 40, 45, 50, 50, 45};

    private FoodQualityRolls() {
    }

    public static FoodQuality roll(RandomSource random, int rank) {
        int normalizedRank = Math.max(0, Math.min(5, rank));
        int value = random.nextInt(100);
        if (value < HIGH_PERCENT[normalizedRank]) {
            return FoodQuality.HIGH;
        }
        if (value < HIGH_PERCENT[normalizedRank] + STANDARD_PERCENT[normalizedRank]) {
            return FoodQuality.STANDARD;
        }
        return FoodQuality.LOW;
    }
}
