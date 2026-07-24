package com.magu1436.craftbound.occupations.foodproducer.quality;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
            double remainingBaseTicks,
            long gameTime,
            double preservationMultiplier
    ) {
        CompoundTag tag = stack.getOrCreateTagElement(ROOT_TAG);
        tag.putInt(QUALITY_TAG, quality.value());
        tag.putString(CATEGORY_TAG, category.name());
        tag.putDouble(REMAINING_TICKS_TAG, Math.max(0.0D, remainingBaseTicks));
        tag.putLong(LAST_UPDATE_TAG, gameTime);
        tag.putDouble(PRESERVATION_MULTIPLIER_TAG, Math.max(1.0D, preservationMultiplier));
    }

    /**
     * 読み込まれていた経過時間を、直前の保存倍率で基礎残り時間へ反映する.
     * 戻り値は、この精算で低下した品質段階数.
     */
    public static int advanceLoadedTime(ItemStack stack, long gameTime, double preservationMultiplier) {
        if (!FoodQualityItems.isQualityTarget(stack)) {
            return 0;
        }
        if (!hasQuality(stack)) {
            initialize(stack, FoodQualityItems.initialQualityForUntracked(stack), gameTime);
            return 0;
        }

        CompoundTag tag = stack.getOrCreateTagElement(ROOT_TAG);
        FoodQuality quality = FoodQuality.fromValue(tag.getInt(QUALITY_TAG));
        FoodQualityCategory category = readCategory(tag, stack);
        double currentMultiplier = Math.max(1.0D, preservationMultiplier);

        if (quality == FoodQuality.SPOILED) {
            tag.putDouble(REMAINING_TICKS_TAG, 0.0D);
            tag.putLong(LAST_UPDATE_TAG, gameTime);
            tag.putDouble(PRESERVATION_MULTIPLIER_TAG, currentMultiplier);
            return 0;
        }

        if (!tag.contains(LAST_UPDATE_TAG, Tag.TAG_ANY_NUMERIC)
                || gameTime < tag.getLong(LAST_UPDATE_TAG)) {
            resetClock(stack, gameTime, currentMultiplier);
            return 0;
        }

        long elapsedTicks = gameTime - tag.getLong(LAST_UPDATE_TAG);
        double previousMultiplier = tag.contains(PRESERVATION_MULTIPLIER_TAG, Tag.TAG_ANY_NUMERIC)
                ? Math.max(1.0D, tag.getDouble(PRESERVATION_MULTIPLIER_TAG))
                : 1.0D;
        double remaining = tag.contains(REMAINING_TICKS_TAG, Tag.TAG_ANY_NUMERIC)
                ? Math.max(0.0D, tag.getDouble(REMAINING_TICKS_TAG))
                : category.stageDurationTicks();
        double consumedBaseTicks = elapsedTicks / previousMultiplier;
        int degradedStages = 0;

        while (quality != FoodQuality.SPOILED && consumedBaseTicks >= remaining) {
            consumedBaseTicks -= remaining;
            quality = FoodQuality.fromValue(quality.value() - 1);
            degradedStages++;
            remaining = quality == FoodQuality.SPOILED ? 0.0D : category.stageDurationTicks();
        }
        if (quality != FoodQuality.SPOILED) {
            remaining = Math.max(0.0D, remaining - consumedBaseTicks);
        }

        tag.putInt(QUALITY_TAG, quality.value());
        tag.putString(CATEGORY_TAG, category.name());
        tag.putDouble(REMAINING_TICKS_TAG, remaining);
        tag.putLong(LAST_UPDATE_TAG, gameTime);
        tag.putDouble(PRESERVATION_MULTIPLIER_TAG, currentMultiplier);
        return degradedStages;
    }

    /** 時計を進めず、次回精算の基準時刻と保存倍率だけを更新する. */
    public static void resetClock(ItemStack stack, long gameTime, double preservationMultiplier) {
        if (!FoodQualityItems.isQualityTarget(stack)) {
            return;
        }
        if (!hasQuality(stack)) {
            initialize(stack, FoodQualityItems.initialQualityForUntracked(stack), gameTime);
            return;
        }
        CompoundTag tag = stack.getOrCreateTagElement(ROOT_TAG);
        tag.putLong(LAST_UPDATE_TAG, gameTime);
        tag.putDouble(PRESERVATION_MULTIPLIER_TAG, Math.max(1.0D, preservationMultiplier));
    }

    public static double getRemainingBaseTicks(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(ROOT_TAG);
        if (tag == null || !tag.contains(REMAINING_TICKS_TAG, Tag.TAG_ANY_NUMERIC)) {
            return 0.0D;
        }
        return Math.max(0.0D, tag.getDouble(REMAINING_TICKS_TAG));
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

    private static FoodQualityCategory readCategory(CompoundTag tag, ItemStack stack) {
        if (tag.contains(CATEGORY_TAG, Tag.TAG_STRING)) {
            try {
                return FoodQualityCategory.valueOf(tag.getString(CATEGORY_TAG));
            } catch (IllegalArgumentException ignored) {
                // 古い、または破損した値は現在のアイテム分類で安全に復旧する.
            }
        }
        return FoodQualityItems.category(stack).orElse(FoodQualityCategory.MATERIAL);
    }
}
