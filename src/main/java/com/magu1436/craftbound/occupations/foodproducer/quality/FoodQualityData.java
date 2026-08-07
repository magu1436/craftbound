package com.magu1436.craftbound.occupations.foodproducer.quality;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

import com.magu1436.craftbound.occupations.foodproducer.processing.FoodCookingData;

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
    private static final String CLOCK_RUNNING_TAG = "clock_running";
    private static final String TEST_FROZEN_TAG = "test_frozen";

    private FoodQualityData() {
    }

    /** 保存された基準値を指定時刻まで進めた、NBTを書き換えない品質状態。 */
    public record Snapshot(
            FoodQuality quality,
            FoodQualityCategory category,
            double remainingBaseTicks,
            double preservationMultiplier,
            boolean clockRunning,
            int degradedStages
    ) {
        public double remainingRealTicks() {
            return remainingBaseTicks * preservationMultiplier;
        }

        public double remainingUntilSpoiledRealTicks() {
            if (quality == FoodQuality.SPOILED) {
                return 0.0D;
            }
            int fullStagesAfterCurrent = Math.max(0, quality.value() - 1);
            return (remainingBaseTicks
                    + fullStagesAfterCurrent * category.stageDurationTicks())
                    * preservationMultiplier;
        }
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

    public static boolean isSpoiled(ItemStack stack) {
        return get(stack).filter(quality -> quality == FoodQuality.SPOILED).isPresent();
    }

    public static boolean containsSpoiled(Iterable<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (isSpoiled(stack)) {
                return true;
            }
        }
        return false;
    }

    public static boolean initialize(ItemStack stack, FoodQuality quality, long gameTime) {
        Optional<FoodQualityCategory> category = FoodQualityItems.category(stack);
        if (category.isEmpty()) {
            return false;
        }

        set(stack, quality, category.get(), category.get().stageDurationTicks(), gameTime, 1.0D);
        stack.getOrCreateTagElement(ROOT_TAG).remove(TEST_FROZEN_TAG);
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
        tag.putBoolean(CLOCK_RUNNING_TAG, true);
    }

    /**
     * 読み込まれていた経過時間を、直前の保存倍率で基礎残り時間へ反映する.
     * 品質段階・保存倍率・時計状態が変化してNBTを書き換えた場合だけtrueを返す.
     */
    public static boolean advanceLoadedTime(ItemStack stack, long gameTime, double preservationMultiplier) {
        if (!FoodQualityItems.isQualityTarget(stack)) {
            return false;
        }
        if (!hasQuality(stack)) {
            initialize(stack, FoodQualityItems.initialQualityForUntracked(stack), gameTime);
            return true;
        }

        CompoundTag tag = stack.getOrCreateTagElement(ROOT_TAG);
        double currentMultiplier = Math.max(1.0D, preservationMultiplier);

        if (tag.getBoolean(TEST_FROZEN_TAG)) {
            return ensureClockState(stack, gameTime, currentMultiplier, false, false);
        }

        if (tag.contains(CLOCK_RUNNING_TAG, Tag.TAG_BYTE) && !tag.getBoolean(CLOCK_RUNNING_TAG)) {
            resetClock(stack, gameTime, currentMultiplier);
            return true;
        }

        if (!tag.contains(LAST_UPDATE_TAG, Tag.TAG_ANY_NUMERIC)
                || gameTime < tag.getLong(LAST_UPDATE_TAG)) {
            resetClock(stack, gameTime, currentMultiplier);
            return true;
        }

        double previousMultiplier = tag.contains(PRESERVATION_MULTIPLIER_TAG, Tag.TAG_ANY_NUMERIC)
                ? Math.max(1.0D, tag.getDouble(PRESERVATION_MULTIPLIER_TAG))
                : 1.0D;
        Snapshot snapshot = calculateSnapshot(stack, tag, gameTime);
        boolean multiplierChanged = Double.compare(previousMultiplier, currentMultiplier) != 0;
        if (snapshot.degradedStages() <= 0 && !multiplierChanged) {
            return false;
        }

        writeSnapshot(stack, snapshot, gameTime, currentMultiplier, true);
        FoodCookingData.lowerPreparedQualityCap(
                stack,
                snapshot.degradedStages(),
                snapshot.quality()
        );
        return true;
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
        boolean frozen = tag.getBoolean(TEST_FROZEN_TAG);
        ensureClockState(
                stack,
                gameTime,
                Math.max(1.0D, preservationMultiplier),
                !frozen,
                true
        );
    }

    /** 読み込まれていない場所や地面上で経過時間を加算しないよう、品質時計を停止する. */
    public static boolean pauseClock(ItemStack stack, long gameTime, double preservationMultiplier) {
        if (!FoodQualityItems.isQualityTarget(stack)) {
            return false;
        }
        if (!hasQuality(stack)) {
            initialize(stack, FoodQualityItems.initialQualityForUntracked(stack), gameTime);
        }
        CompoundTag tag = stack.getOrCreateTagElement(ROOT_TAG);
        double currentMultiplier = Math.max(1.0D, preservationMultiplier);
        if (tag.contains(CLOCK_RUNNING_TAG, Tag.TAG_BYTE)
                && !tag.getBoolean(CLOCK_RUNNING_TAG)
                && Double.compare(getPreservationMultiplier(stack), currentMultiplier) == 0) {
            return false;
        }

        Snapshot snapshot = calculateSnapshot(stack, tag, gameTime);
        writeSnapshot(stack, snapshot, gameTime, currentMultiplier, false);
        FoodCookingData.lowerPreparedQualityCap(
                stack,
                snapshot.degradedStages(),
                snapshot.quality()
        );
        return true;
    }

    public static Optional<Snapshot> snapshot(ItemStack stack, long gameTime) {
        CompoundTag tag = stack.getTagElement(ROOT_TAG);
        return tag == null ? Optional.empty()
                : Optional.of(calculateSnapshot(stack, tag, gameTime));
    }

    public static double getRemainingBaseTicks(ItemStack stack, long gameTime) {
        return snapshot(stack, gameTime).map(Snapshot::remainingBaseTicks).orElse(0.0D);
    }

    /** 現在の保存倍率を反映した、画面表示用の残り実時間をtick単位で返す. */
    public static double getRemainingRealTicks(ItemStack stack, long gameTime) {
        return snapshot(stack, gameTime).map(Snapshot::remainingRealTicks).orElse(0.0D);
    }

    /** 現在品質から腐敗までの、現在の保存倍率を反映した残り実時間を返す。 */
    public static double getRemainingUntilSpoiledRealTicks(ItemStack stack, long gameTime) {
        return snapshot(stack, gameTime)
                .map(Snapshot::remainingUntilSpoiledRealTicks)
                .orElse(0.0D);
    }

    public static Optional<FoodQualityCategory> getCategory(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(ROOT_TAG);
        if (tag == null) {
            return FoodQualityItems.category(stack);
        }
        return Optional.of(readCategory(tag, stack));
    }

    public static double getPreservationMultiplier(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(ROOT_TAG);
        if (tag == null || !tag.contains(PRESERVATION_MULTIPLIER_TAG, Tag.TAG_ANY_NUMERIC)) {
            return 1.0D;
        }
        return Math.max(1.0D, tag.getDouble(PRESERVATION_MULTIPLIER_TAG));
    }

    public static boolean isClockRunning(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(ROOT_TAG);
        return tag == null
                || !tag.contains(CLOCK_RUNNING_TAG, Tag.TAG_BYTE)
                || tag.getBoolean(CLOCK_RUNNING_TAG);
    }

    /** OP向けテストコマンドで、品質を時間経過から完全に固定する。 */
    public static void freezeForTesting(ItemStack stack, long gameTime) {
        if (!hasQuality(stack)) {
            return;
        }
        CompoundTag tag = stack.getOrCreateTagElement(ROOT_TAG);
        tag.putBoolean(TEST_FROZEN_TAG, true);
        tag.putLong(LAST_UPDATE_TAG, gameTime);
        tag.putBoolean(CLOCK_RUNNING_TAG, false);
    }

    public static void unfreezeForTesting(ItemStack stack, long gameTime) {
        CompoundTag tag = stack.getTagElement(ROOT_TAG);
        if (tag == null) {
            return;
        }
        tag.remove(TEST_FROZEN_TAG);
        tag.putLong(LAST_UPDATE_TAG, gameTime);
        tag.putBoolean(CLOCK_RUNNING_TAG, true);
    }

    /** OP向けテストで、現在品質が低下するまでの基礎残り時間を指定する。 */
    public static boolean setRemainingForTesting(
            ItemStack stack,
            long remainingTicks,
            long gameTime
    ) {
        CompoundTag tag = stack.getTagElement(ROOT_TAG);
        if (tag == null || remainingTicks <= 0L) {
            return false;
        }
        Snapshot snapshot = calculateSnapshot(stack, tag, gameTime);
        tag.remove(TEST_FROZEN_TAG);
        writeSnapshot(
                stack,
                new Snapshot(
                        snapshot.quality(),
                        snapshot.category(),
                        remainingTicks,
                        snapshot.preservationMultiplier(),
                        true,
                        0
                ),
                gameTime,
                snapshot.preservationMultiplier(),
                true
        );
        return true;
    }

    public static boolean isFrozenForTesting(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(ROOT_TAG);
        return tag != null && tag.getBoolean(TEST_FROZEN_TAG);
    }

    /**
     * 品質時計だけが異なる2スタックを、バニラの統合判定より広い意味で比較する.
     * 品質、分類、その他MODのNBTおよびCapabilityは一致していなければならない.
     */
    public static boolean isMergeCompatible(ItemStack first, ItemStack second) {
        if (first.isEmpty() || second.isEmpty() || !hasQuality(first) || !hasQuality(second)) {
            return false;
        }

        ItemStack normalizedFirst = first.copy();
        ItemStack normalizedSecond = second.copy();
        removeClockData(normalizedFirst);
        removeClockData(normalizedSecond);
        return ItemStack.isSameItemSameTags(normalizedFirst, normalizedSecond);
    }

    /**
     * 2スタックを現在時刻まで精算し、統合可能なら短い方の残り時間へ揃える.
     * 戻り値がtrueの場合、直後のバニラ統合判定でも同じNBTとして扱える.
     */
    public static boolean prepareForMerge(
            ItemStack first,
            ItemStack second,
            long gameTime,
            double preservationMultiplier
    ) {
        advanceLoadedTime(first, gameTime, preservationMultiplier);
        advanceLoadedTime(second, gameTime, preservationMultiplier);
        if (!isMergeCompatible(first, second)) {
            return false;
        }

        CompoundTag firstTag = first.getOrCreateTagElement(ROOT_TAG);
        FoodQuality quality = FoodQuality.fromValue(firstTag.getInt(QUALITY_TAG));
        FoodQualityCategory category = readCategory(firstTag, first);
        double remaining = Math.min(
                getRemainingBaseTicks(first, gameTime),
                getRemainingBaseTicks(second, gameTime)
        );
        set(first, quality, category, remaining, gameTime, preservationMultiplier);
        set(second, quality, category, remaining, gameTime, preservationMultiplier);
        return true;
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

    private static Snapshot calculateSnapshot(ItemStack stack, CompoundTag tag, long gameTime) {
        FoodQuality quality = FoodQuality.fromValue(tag.getInt(QUALITY_TAG));
        FoodQualityCategory category = readCategory(tag, stack);
        double multiplier = tag.contains(PRESERVATION_MULTIPLIER_TAG, Tag.TAG_ANY_NUMERIC)
                ? Math.max(1.0D, tag.getDouble(PRESERVATION_MULTIPLIER_TAG))
                : 1.0D;
        double remaining = tag.contains(REMAINING_TICKS_TAG, Tag.TAG_ANY_NUMERIC)
                ? Math.max(0.0D, tag.getDouble(REMAINING_TICKS_TAG))
                : category.stageDurationTicks();
        boolean running = !tag.contains(CLOCK_RUNNING_TAG, Tag.TAG_BYTE)
                || tag.getBoolean(CLOCK_RUNNING_TAG);
        boolean frozen = tag.getBoolean(TEST_FROZEN_TAG);
        if (!running || frozen || quality == FoodQuality.SPOILED
                || !tag.contains(LAST_UPDATE_TAG, Tag.TAG_ANY_NUMERIC)
                || gameTime < tag.getLong(LAST_UPDATE_TAG)) {
            return new Snapshot(
                    quality,
                    category,
                    quality == FoodQuality.SPOILED ? 0.0D : remaining,
                    multiplier,
                    running && !frozen,
                    0
            );
        }

        double consumedBaseTicks = (gameTime - tag.getLong(LAST_UPDATE_TAG)) / multiplier;
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
        return new Snapshot(
                quality,
                category,
                remaining,
                multiplier,
                true,
                degradedStages
        );
    }

    private static void writeSnapshot(
            ItemStack stack,
            Snapshot snapshot,
            long gameTime,
            double preservationMultiplier,
            boolean running
    ) {
        CompoundTag tag = stack.getOrCreateTagElement(ROOT_TAG);
        tag.putInt(QUALITY_TAG, snapshot.quality().value());
        tag.putString(CATEGORY_TAG, snapshot.category().name());
        tag.putDouble(REMAINING_TICKS_TAG, snapshot.remainingBaseTicks());
        tag.putLong(LAST_UPDATE_TAG, gameTime);
        tag.putDouble(PRESERVATION_MULTIPLIER_TAG, Math.max(1.0D, preservationMultiplier));
        tag.putBoolean(CLOCK_RUNNING_TAG, running);
    }

    private static boolean ensureClockState(
            ItemStack stack,
            long gameTime,
            double preservationMultiplier,
            boolean running,
            boolean forceReanchor
    ) {
        CompoundTag tag = stack.getOrCreateTagElement(ROOT_TAG);
        boolean sameRunning = tag.contains(CLOCK_RUNNING_TAG, Tag.TAG_BYTE)
                && tag.getBoolean(CLOCK_RUNNING_TAG) == running;
        boolean sameMultiplier = Double.compare(
                getPreservationMultiplier(stack),
                preservationMultiplier
        ) == 0;
        if (!forceReanchor && sameRunning && sameMultiplier) {
            return false;
        }
        tag.putLong(LAST_UPDATE_TAG, gameTime);
        tag.putDouble(PRESERVATION_MULTIPLIER_TAG, preservationMultiplier);
        tag.putBoolean(CLOCK_RUNNING_TAG, running);
        return true;
    }

    private static void removeClockData(ItemStack stack) {
        CompoundTag root = stack.getTagElement(ROOT_TAG);
        if (root == null) {
            return;
        }
        root.remove(REMAINING_TICKS_TAG);
        root.remove(LAST_UPDATE_TAG);
        root.remove(PRESERVATION_MULTIPLIER_TAG);
        root.remove(CLOCK_RUNNING_TAG);
    }
}
