package com.magu1436.craftbound.occupations.foodproducer.quality;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** ItemStackへ保存する品質データの読み書き窓口. */
public final class FoodQualityData {

    private static final String ROOT_TAG = "craftbound_food_quality";
    private static final String QUALITY_TAG = "quality";
    private static final String CATEGORY_TAG = "category";
    private static final String REMAINING_TICKS_TAG = "remaining_base_ticks";
    private static final String LAST_UPDATE_TAG = "last_update_game_time";
    private static final String PRESERVATION_MULTIPLIER_TAG = "preservation_multiplier";

    private FoodQualityData() {
    }

    public static boolean hasQuality(ItemStack stack) {
        return stack.getTagElement(ROOT_TAG) != null;
    }

    public static Optional<FoodQuality> get(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(ROOT_TAG);
        if (tag == null) {
            return Optional.empty();
        }
        return Optional.of(FoodQuality.fromValue(tag.getInt(QUALITY_TAG)));
    }

    public static FoodQuality getOrStandard(ItemStack stack) {
        return get(stack).orElse(FoodQuality.STANDARD);
    }

    public static boolean initialize(ItemStack stack, FoodQuality quality, long gameTime) {
        Optional<FoodQualityCategory> category = FoodQualityItems.category(stack);
        if (category.isEmpty()) {
            return false;
        }

        set(stack, quality, category.get(), category.get().stageDurationTicks(), gameTime, 1.0D);
        return true;
    }

    public static void set(
            ItemStack stack,
            FoodQuality quality,
            FoodQualityCategory category,
            long remainingBaseTicks,
            long gameTime,
            double preservationMultiplier
    ) {
        CompoundTag tag = stack.getOrCreateTagElement(ROOT_TAG);
        tag.putInt(QUALITY_TAG, quality.value());
        tag.putString(CATEGORY_TAG, category.name());
        tag.putLong(REMAINING_TICKS_TAG, Math.max(0L, remainingBaseTicks));
        tag.putLong(LAST_UPDATE_TAG, gameTime);
        tag.putDouble(PRESERVATION_MULTIPLIER_TAG, Math.max(1.0D, preservationMultiplier));
    }

    /** 品質対象材料の最低品質を出力へ継承する. */
    public static boolean inheritMinimum(Collection<ItemStack> inputs, ItemStack output, long gameTime) {
        Optional<FoodQuality> minimum = inputs.stream()
                .filter(FoodQualityItems::isQualityTarget)
                .map(FoodQualityData::getOrStandard)
                .min(Comparator.comparingInt(FoodQuality::value));

        return minimum.filter(quality -> FoodQualityItems.isQualityTarget(output))
                .map(quality -> initialize(output, quality, gameTime))
                .orElse(false);
    }
}
