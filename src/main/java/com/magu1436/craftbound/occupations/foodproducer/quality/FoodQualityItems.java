package com.magu1436.craftbound.occupations.foodproducer.quality;

import java.util.Optional;
import java.util.Set;

import com.magu1436.craftbound.occupations.foodproducer.processing.FoodIntermediateItem;
import com.magu1436.craftbound.occupations.foodproducer.processing.FoodCookingData;
import com.magu1436.craftbound.occupations.foodproducer.processing.FoodDishItem;
import com.magu1436.craftbound.occupations.foodproducer.processing.PreparedIngredientSetItem;
import com.magu1436.craftbound.occupations.foodproducer.foraging.RegionalIngredientItem;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** 初期品質対象となるバニラ作物、畜産物、料理材料とバニラ加工品. */
public final class FoodQualityItems {

    private static final Set<Item> MATERIALS = Set.of(
            Items.WHEAT,
            Items.CARROT,
            Items.POTATO,
            Items.BEETROOT,
            Items.PUMPKIN,
            Items.MELON_SLICE,
            Items.SUGAR_CANE,
            Items.COCOA_BEANS,
            Items.SWEET_BERRIES,
            Items.GLOW_BERRIES,
            Items.APPLE,
            Items.BROWN_MUSHROOM,
            Items.RED_MUSHROOM,
            Items.SUGAR,
            Items.BEEF,
            Items.PORKCHOP,
            Items.MUTTON,
            Items.CHICKEN,
            Items.RABBIT,
            Items.MILK_BUCKET,
            Items.EGG
    );

    private static final Set<Item> DISHES = Set.of(
            Items.BREAD,
            Items.BAKED_POTATO,
            Items.COOKED_BEEF,
            Items.COOKED_PORKCHOP,
            Items.COOKED_MUTTON,
            Items.COOKED_CHICKEN,
            Items.COOKED_RABBIT,
            Items.COOKIE,
            Items.PUMPKIN_PIE,
            Items.MUSHROOM_STEW,
            Items.BEETROOT_SOUP,
            Items.RABBIT_STEW,
            Items.CAKE
    );

    private FoodQualityItems() {
    }

    public static Optional<FoodQualityCategory> category(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof RegionalIngredientItem) {
            return Optional.of(FoodQualityCategory.REGIONAL_INGREDIENT);
        }
        if (MATERIALS.contains(item)
                || item instanceof FoodIntermediateItem
                || item instanceof PreparedIngredientSetItem) {
            return Optional.of(FoodQualityCategory.MATERIAL);
        }
        if (item instanceof FoodDishItem) {
            return Optional.of(FoodCookingData.isPreserved(stack)
                    ? FoodQualityCategory.PRESERVED_FOOD
                    : FoodQualityCategory.DISH);
        }
        if (DISHES.contains(item)) {
            return Optional.of(FoodQualityCategory.DISH);
        }
        return Optional.empty();
    }

    public static boolean isQualityTarget(ItemStack stack) {
        return category(stack).isPresent();
    }

    /** 固定規則を優先し、取得経路不明の既存品は標準品質とする. */
    public static FoodQuality initialQualityForUntracked(ItemStack stack) {
        if (stack.is(Items.EGG) || stack.is(Items.MILK_BUCKET)) {
            return FoodQuality.HIGH;
        }
        if (stack.is(Items.MUSHROOM_STEW)) {
            return FoodQuality.LOW;
        }
        return FoodQuality.STANDARD;
    }
}
