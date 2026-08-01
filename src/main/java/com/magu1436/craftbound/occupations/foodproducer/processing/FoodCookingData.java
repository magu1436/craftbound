package com.magu1436.craftbound.occupations.foodproducer.processing;

import java.util.Optional;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQuality;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** 調理前素材セットから完成料理まで引き継ぐ、料理共通データ。 */
public final class FoodCookingData {

    private static final String ROOT = "craftbound_cooking";
    private static final String RECIPE_ID = "recipe_id";
    private static final String NAME_KEY = "name_key";
    private static final String NUTRITION = "nutrition";
    private static final String SATURATION_GAIN = "saturation_gain";
    private static final String EFFECT_ID = "effect_id";
    private static final String EFFECT_AMPLIFIER = "effect_amplifier";
    private static final String EFFECT_DURATION = "effect_duration";
    private static final String QUALITY_CAP = "quality_cap";
    private static final String RATING = "rating";
    private static final String PRESERVED = "preserved";
    private static final String EXPERIENCE = "experience";
    private static final String RETURN_ITEM = "return_item";
    private static final String RETURN_COUNT = "return_count";
    private static final String DIET_VALUES = "diet_values";

    private FoodCookingData() {
    }

    public static ItemStack createTestPreparedSet(FoodQuality baseQuality, long gameTime) {
        ItemStack stack = new ItemStack(Craftbound.PREPARED_INGREDIENT_SET.get());
        write(
                stack,
                ResourceLocation.fromNamespaceAndPath(Craftbound.MODID, "test_meal"),
                "item.craftbound.cooking_test_dish",
                6,
                5.0F,
                ResourceLocation.withDefaultNamespace("speed"),
                0,
                2 * 60 * 20,
                FoodQuality.HIGH,
                FoodIntermediateData.SUCCESS_RATING,
                false,
                5
        );
        setReturnedContainer(stack, Items.BOWL, 1);
        FoodQualityData.initialize(stack, baseQuality, gameTime);
        return stack;
    }

    public static ItemStack createDish(ItemStack preparedSet) {
        if (!isPreparedSet(preparedSet)) {
            return ItemStack.EMPTY;
        }
        ItemStack output = new ItemStack(Craftbound.FOOD_DISH.get());
        CompoundTag cooking = preparedSet.getTagElement(ROOT);
        if (cooking != null) {
            output.addTagElement(ROOT, cooking.copy());
        }
        return output;
    }

    public static void write(
            ItemStack stack,
            ResourceLocation recipeId,
            String nameKey,
            int nutrition,
            float saturationGain,
            ResourceLocation effectId,
            int effectAmplifier,
            int effectDuration,
            FoodQuality qualityCap,
            int rating,
            boolean preserved,
            int experience
    ) {
        CompoundTag data = stack.getOrCreateTagElement(ROOT);
        data.putString(RECIPE_ID, recipeId.toString());
        data.putString(NAME_KEY, nameKey);
        data.putInt(NUTRITION, Math.max(1, nutrition));
        data.putFloat(SATURATION_GAIN, Math.max(0.0F, saturationGain));
        data.putString(EFFECT_ID, effectId.toString());
        data.putInt(EFFECT_AMPLIFIER, Math.max(0, effectAmplifier));
        data.putInt(EFFECT_DURATION, Math.max(0, effectDuration));
        data.putInt(QUALITY_CAP, qualityCap.value());
        data.putInt(RATING, Math.max(0, Math.min(3, rating)));
        data.putBoolean(PRESERVED, preserved);
        data.putInt(EXPERIENCE, Math.max(0, experience));
        data.put(DIET_VALUES, new CompoundTag());
    }

    public static boolean isPreparedSet(ItemStack stack) {
        return stack.is(Craftbound.PREPARED_INGREDIENT_SET.get()) && stack.getTagElement(ROOT) != null;
    }

    public static Optional<ResourceLocation> recipeId(ItemStack stack) {
        CompoundTag data = stack.getTagElement(ROOT);
        return data == null ? Optional.empty()
                : Optional.ofNullable(ResourceLocation.tryParse(data.getString(RECIPE_ID)));
    }

    public static String nameKey(ItemStack stack) {
        CompoundTag data = stack.getTagElement(ROOT);
        return data == null || data.getString(NAME_KEY).isBlank()
                ? "item.craftbound.food_dish"
                : data.getString(NAME_KEY);
    }

    public static int nutrition(ItemStack stack) {
        CompoundTag data = stack.getTagElement(ROOT);
        return data == null ? 1 : Math.max(1, data.getInt(NUTRITION));
    }

    public static float saturationGain(ItemStack stack) {
        CompoundTag data = stack.getTagElement(ROOT);
        return data == null ? 0.0F : Math.max(0.0F, data.getFloat(SATURATION_GAIN));
    }

    public static Optional<ResourceLocation> effectId(ItemStack stack) {
        CompoundTag data = stack.getTagElement(ROOT);
        return data == null ? Optional.empty()
                : Optional.ofNullable(ResourceLocation.tryParse(data.getString(EFFECT_ID)));
    }

    public static int effectAmplifier(ItemStack stack) {
        CompoundTag data = stack.getTagElement(ROOT);
        return data == null ? 0 : Math.max(0, data.getInt(EFFECT_AMPLIFIER));
    }

    public static int effectDuration(ItemStack stack) {
        CompoundTag data = stack.getTagElement(ROOT);
        return data == null ? 0 : Math.max(0, data.getInt(EFFECT_DURATION));
    }

    public static FoodQuality qualityCap(ItemStack stack) {
        CompoundTag data = stack.getTagElement(ROOT);
        return data == null ? FoodQuality.STANDARD : FoodQuality.fromValue(data.getInt(QUALITY_CAP));
    }

    public static int rating(ItemStack stack) {
        CompoundTag data = stack.getTagElement(ROOT);
        return data == null ? FoodIntermediateData.SUCCESS_RATING
                : Math.max(0, Math.min(3, data.getInt(RATING)));
    }

    public static boolean isPreserved(ItemStack stack) {
        CompoundTag data = stack.getTagElement(ROOT);
        return data != null && data.getBoolean(PRESERVED);
    }

    public static int experience(ItemStack stack) {
        CompoundTag data = stack.getTagElement(ROOT);
        return data == null ? 0 : Math.max(0, data.getInt(EXPERIENCE));
    }

    public static void setReturnedContainer(ItemStack stack, Item item, int count) {
        CompoundTag data = stack.getOrCreateTagElement(ROOT);
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        data.putString(RETURN_ITEM, itemId.toString());
        data.putInt(RETURN_COUNT, Math.max(1, count));
    }

    public static ItemStack returnedContainer(ItemStack stack) {
        CompoundTag data = stack.getTagElement(ROOT);
        if (data == null || !data.contains(RETURN_ITEM)) {
            return ItemStack.EMPTY;
        }
        ResourceLocation itemId = ResourceLocation.tryParse(data.getString(RETURN_ITEM));
        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(itemId), Math.max(1, data.getInt(RETURN_COUNT)));
    }

    public static void setDietValue(ItemStack stack, String category, float value) {
        if (category == null || category.isBlank()) {
            return;
        }
        CompoundTag cooking = stack.getOrCreateTagElement(ROOT);
        CompoundTag diet = cooking.getCompound(DIET_VALUES);
        diet.putFloat(category, Math.max(0.0F, value));
        cooking.put(DIET_VALUES, diet);
    }

    public static float dietValue(ItemStack stack, String category) {
        CompoundTag cooking = stack.getTagElement(ROOT);
        if (cooking == null || category == null || category.isBlank()) {
            return 0.0F;
        }
        return Math.max(0.0F, cooking.getCompound(DIET_VALUES).getFloat(category));
    }

    /** 素材セットの品質低下に合わせ、最終加熱で使用できる品質上限も低下させる。 */
    public static void lowerPreparedQualityCap(ItemStack stack, int degradedStages, FoodQuality currentQuality) {
        if (!isPreparedSet(stack) || degradedStages <= 0) {
            return;
        }
        CompoundTag data = stack.getOrCreateTagElement(ROOT);
        int lowered = qualityCap(stack).value() - degradedStages;
        data.putInt(QUALITY_CAP, Math.max(currentQuality.value(), lowered));
    }
}
