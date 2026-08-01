package com.magu1436.craftbound.occupations.foodproducer.quality;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 品質による食事性能の初期プレイテスト倍率を提供する. */
public final class FoodQualityEffects {

    private FoodQualityEffects() {
    }

    public static double foodMultiplier(FoodQuality quality) {
        return switch (quality) {
            case HIGH -> 1.25D;
            case STANDARD -> 1.0D;
            case LOW -> 0.75D;
            case SPOILED -> 0.0D;
        };
    }

    public static int adjustedNutrition(ItemStack stack, FoodQuality quality) {
        return adjustedNutrition(stack, null, quality);
    }

    public static int adjustedNutrition(
            ItemStack stack,
            LivingEntity entity,
            FoodQuality quality
    ) {
        FoodProperties food = stack.getFoodProperties(entity);
        if (food == null || quality == FoodQuality.SPOILED) {
            return 0;
        }
        return Math.max(1, (int) Math.round(food.getNutrition() * foodMultiplier(quality)));
    }

    /** バニラのnutrition×saturationModifier×2へ、品質倍率を1回だけ適用する. */
    public static float adjustedSaturationGain(ItemStack stack, FoodQuality quality) {
        return adjustedSaturationGain(stack, null, quality);
    }

    public static float adjustedSaturationGain(
            ItemStack stack,
            LivingEntity entity,
            FoodQuality quality
    ) {
        FoodProperties food = stack.getFoodProperties(entity);
        if (food == null || quality == FoodQuality.SPOILED) {
            return 0.0F;
        }
        double baseGain = food.getNutrition() * food.getSaturationModifier() * 2.0D;
        return (float) (baseGain * foodMultiplier(quality));
    }

}
