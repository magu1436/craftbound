package com.magu1436.craftbound.occupations.foodproducer.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.registry.CraftboundItems;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQuality;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
    private static final String EFFECTS = "effects";
    private static final String EFFECT_SHOW_PARTICLES = "show_particles";
    private static final String EFFECT_SHOW_ICON = "show_icon";
    private static final String QUALITY_CAP = "quality_cap";
    private static final String RATING = "rating";
    private static final String PRESERVED = "preserved";
    private static final String EXPERIENCE = "experience";
    private static final String REQUIRED_RECIPE_RANK = "required_recipe_rank";
    private static final String RETURN_ITEM = "return_item";
    private static final String RETURN_COUNT = "return_count";
    private static final String DIET_VALUES = "diet_values";

    private FoodCookingData() {
    }

    public static ItemStack createTestPreparedSet(FoodQuality baseQuality, long gameTime) {
        ItemStack stack = new ItemStack(CraftboundItems.PREPARED_INGREDIENT_SET.get());
        write(
                stack,
                ResourceLocation.fromNamespaceAndPath(Craftbound.MODID, "test_meal"),
                "item.craftbound.cooking_test_dish",
                6,
                5.0F,
                List.of(new FoodCookingEffect(
                        ResourceLocation.withDefaultNamespace("speed"),
                        0,
                        2 * 60 * 20,
                        true,
                        true
                )),
                FoodQuality.HIGH,
                FoodIntermediateData.SUCCESS_RATING,
                false,
                5,
                0
        );
        setReturnedContainer(stack, Items.BOWL, 1);
        FoodQualityData.initialize(stack, baseQuality, gameTime);
        return stack;
    }

    public static ItemStack createDish(ItemStack preparedSet) {
        if (!isPreparedSet(preparedSet)) {
            return ItemStack.EMPTY;
        }
        ItemStack output = new ItemStack(CraftboundItems.FOOD_DISH.get());
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
            List<FoodCookingEffect> effects,
            FoodQuality qualityCap,
            int rating,
            boolean preserved,
            int experience,
            int requiredRecipeRank
    ) {
        CompoundTag data = stack.getOrCreateTagElement(ROOT);
        data.putString(RECIPE_ID, recipeId.toString());
        data.putString(NAME_KEY, nameKey);
        data.putInt(NUTRITION, Math.max(1, nutrition));
        data.putFloat(SATURATION_GAIN, Math.max(0.0F, saturationGain));
        writeEffects(data, effects);
        data.putInt(QUALITY_CAP, qualityCap.value());
        data.putInt(RATING, Math.max(0, Math.min(3, rating)));
        data.putBoolean(PRESERVED, preserved);
        data.putInt(EXPERIENCE, Math.max(0, experience));
        data.putInt(REQUIRED_RECIPE_RANK, Math.max(0, Math.min(5, requiredRecipeRank)));
        data.put(DIET_VALUES, new CompoundTag());
    }

    public static boolean isPreparedSet(ItemStack stack) {
        return stack.is(CraftboundItems.PREPARED_INGREDIENT_SET.get()) && stack.getTagElement(ROOT) != null;
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
        return effects(stack).stream().findFirst().map(FoodCookingEffect::id);
    }

    public static int effectAmplifier(ItemStack stack) {
        return effects(stack).stream().findFirst()
                .map(FoodCookingEffect::amplifier).orElse(0);
    }

    public static int effectDuration(ItemStack stack) {
        return effects(stack).stream().findFirst()
                .map(FoodCookingEffect::durationTicks).orElse(0);
    }

    /** 新形式を優先し、v2.0以前の単一効果NBTも読み込む。 */
    public static List<FoodCookingEffect> effects(ItemStack stack) {
        CompoundTag data = stack.getTagElement(ROOT);
        if (data == null) {
            return List.of();
        }
        if (data.contains(EFFECTS, Tag.TAG_LIST)) {
            ListTag stored = data.getList(EFFECTS, Tag.TAG_COMPOUND);
            List<FoodCookingEffect> result = new ArrayList<>(stored.size());
            for (int index = 0; index < stored.size(); index++) {
                CompoundTag effect = stored.getCompound(index);
                ResourceLocation id = ResourceLocation.tryParse(effect.getString(EFFECT_ID));
                if (id == null) {
                    continue;
                }
                result.add(new FoodCookingEffect(
                        id,
                        effect.getInt(EFFECT_AMPLIFIER),
                        effect.getInt(EFFECT_DURATION),
                        effect.getBoolean(EFFECT_SHOW_PARTICLES),
                        effect.getBoolean(EFFECT_SHOW_ICON)
                ));
            }
            return List.copyOf(result);
        }

        ResourceLocation legacyId = ResourceLocation.tryParse(data.getString(EFFECT_ID));
        if (legacyId == null || data.getInt(EFFECT_DURATION) <= 0) {
            return List.of();
        }
        return List.of(new FoodCookingEffect(
                legacyId,
                data.getInt(EFFECT_AMPLIFIER),
                data.getInt(EFFECT_DURATION),
                true,
                true
        ));
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

    public static int requiredRecipeRank(ItemStack stack) {
        CompoundTag data = stack.getTagElement(ROOT);
        return data == null ? 0 : Math.max(0, Math.min(5, data.getInt(REQUIRED_RECIPE_RANK)));
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

    private static void writeEffects(CompoundTag data, List<FoodCookingEffect> effects) {
        ListTag stored = new ListTag();
        for (FoodCookingEffect effect : effects == null ? List.<FoodCookingEffect>of() : effects) {
            if (effect.durationTicks() <= 0) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putString(EFFECT_ID, effect.id().toString());
            entry.putInt(EFFECT_AMPLIFIER, effect.amplifier());
            entry.putInt(EFFECT_DURATION, effect.durationTicks());
            entry.putBoolean(EFFECT_SHOW_PARTICLES, effect.showParticles());
            entry.putBoolean(EFFECT_SHOW_ICON, effect.showIcon());
            stored.add(entry);
        }
        data.put(EFFECTS, stored);
        data.remove(EFFECT_ID);
        data.remove(EFFECT_AMPLIFIER);
        data.remove(EFFECT_DURATION);
    }
}
