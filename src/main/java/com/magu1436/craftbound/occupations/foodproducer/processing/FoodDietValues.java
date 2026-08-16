package com.magu1436.craftbound.occupations.foodproducer.processing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.magu1436.craftbound.registry.CraftboundItems;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** FoodProducer材料から加工品へ継承するDietカテゴリ値。 */
public final class FoodDietValues {

    public static final String GRAINS = "grains";
    public static final String VEGETABLES = "vegetables";
    public static final String FRUITS = "fruits";
    public static final String PROTEINS = "proteins";
    public static final String SUGARS = "sugars";
    public static final List<String> CATEGORIES = List.of(
            GRAINS, VEGETABLES, FRUITS, PROTEINS, SUGARS
    );
    public static final float MAX_VALUE = 5.0F;

    private FoodDietValues() {
    }

    public static Map<String, Float> values(ItemStack stack) {
        Map<String, Float> stored = FoodCookingData.dietValues(stack);
        if (!stored.isEmpty()) {
            return stored;
        }
        Map<String, Float> direct = baseValues(stack.getItem());
        if (!direct.isEmpty()) {
            return direct;
        }
        ResourceLocation source = FoodIntermediateData.getSource(stack).orElse(null);
        return source == null
                ? Map.of()
                : baseValues(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(source));
    }

    /** 入力スタックの個数分を加算し、基礎出力1個あたりの値として保存する。 */
    public static void inherit(List<ItemStack> inputs, ItemStack output) {
        if (output.isEmpty()) {
            return;
        }
        List<Portion> portions = new ArrayList<>();
        for (ItemStack input : inputs) {
            if (input.isEmpty()) {
                continue;
            }
            portions.add(new Portion(values(input), input.getCount()));
        }
        FoodCookingData.setDietValues(output, inheritedValues(portions, output.getCount()));
    }

    /** Minecraft登録に依存しないDiet継承計算。 */
    public static Map<String, Float> inheritedValues(List<Portion> inputs, int outputCount) {
        Map<String, Float> totals = emptyValues();
        for (Portion input : inputs) {
            input.values().forEach((category, value) -> {
                if (value != null && value > 0.0F) {
                    totals.computeIfPresent(
                            category,
                            (ignored, current) -> current + value * input.count()
                    );
                }
            });
        }
        int divisor = Math.max(1, outputCount);
        totals.replaceAll((category, value) -> Math.min(MAX_VALUE, value / divisor));
        totals.values().removeIf(value -> value <= 0.0F);
        return Map.copyOf(totals);
    }

    public static float scaledGain(
            float inheritedPoints,
            double gainPerPoint,
            double mealMultiplier,
            double maximumGain
    ) {
        if (inheritedPoints <= 0.0F || gainPerPoint <= 0.0D
                || mealMultiplier <= 0.0D || maximumGain <= 0.0D) {
            return 0.0F;
        }
        return (float) Math.min(
                maximumGain,
                inheritedPoints * gainPerPoint * mealMultiplier
        );
    }

    private static Map<String, Float> baseValues(Item item) {
        Map<String, Float> result = emptyValues();
        if (item == Items.WHEAT) {
            result.put(GRAINS, 1.0F);
        } else if (item == Items.CARROT || item == Items.POTATO || item == Items.BEETROOT
                || item == Items.PUMPKIN || item == Items.BROWN_MUSHROOM || item == Items.RED_MUSHROOM) {
            result.put(VEGETABLES, 1.0F);
        } else if (item == Items.APPLE || item == Items.SWEET_BERRIES || item == Items.GLOW_BERRIES) {
            result.put(FRUITS, 1.0F);
        } else if (item == Items.BEEF || item == Items.PORKCHOP || item == Items.MUTTON
                || item == Items.CHICKEN || item == Items.RABBIT || item == Items.EGG
                || item == Items.MILK_BUCKET) {
            result.put(PROTEINS, 1.0F);
        } else if (item == Items.SUGAR || item == Items.COCOA_BEANS) {
            result.put(SUGARS, 1.0F);
        } else if (item == CraftboundItems.WHEAT_FLOUR.get() || item == CraftboundItems.DOUGH.get()) {
            result.put(GRAINS, 2.0F);
        } else if (item == CraftboundItems.SLICED_MEAT.get() || item == CraftboundItems.DRIED_MEAT.get()) {
            result.put(PROTEINS, 0.5F);
        } else if (item == CraftboundItems.GROUND_MEAT.get()) {
            result.put(PROTEINS, 1.0F);
        } else if (item == CraftboundItems.CHOPPED_VEGETABLE.get()) {
            result.put(VEGETABLES, 0.5F);
        } else if (item == CraftboundItems.DRIED_VEGETABLE.get()) {
            result.put(VEGETABLES, 1.0F);
        } else if (item == CraftboundItems.FRUIT_PIECES.get()) {
            result.put(FRUITS, 0.5F);
        } else if (item == CraftboundItems.DRIED_FRUIT.get()) {
            result.put(FRUITS, 1.0F);
        } else if (item == CraftboundItems.JUNIPER_BERRY.get()
                || item == CraftboundItems.CACTUS_FIG.get()
                || item == CraftboundItems.ICE_CRYSTAL_BERRY.get()) {
            result.put(FRUITS, 1.0F);
        } else if (item == CraftboundItems.WILD_GARLIC.get()
                || item == CraftboundItems.WATER_CELERY.get()
                || item == CraftboundItems.JUNGLE_PEPPER.get()
                || item == CraftboundItems.ALPINE_LEEK.get()
                || item == CraftboundItems.MUSHROOM_TRUFFLE.get()) {
            result.put(VEGETABLES, 1.0F);
        }
        result.values().removeIf(value -> value <= 0.0F);
        return Map.copyOf(result);
    }

    private static Map<String, Float> emptyValues() {
        Map<String, Float> result = new LinkedHashMap<>();
        CATEGORIES.forEach(category -> result.put(category, 0.0F));
        return result;
    }

    public record Portion(Map<String, Float> values, int count) {

        public Portion {
            values = values == null ? Map.of() : Map.copyOf(values);
            count = Math.max(0, count);
        }
    }
}
