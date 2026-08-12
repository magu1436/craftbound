package com.magu1436.craftbound.occupations.foodproducer.processing;

import java.util.Optional;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** 中間素材の元材料種と料理操作評価を保存する。 */
public final class FoodIntermediateData {

    private static final String ROOT = "craftbound_processing";
    private static final String SOURCE = "source_item";
    private static final String RATING = "rating";
    public static final int SUCCESS_RATING = 1;

    private FoodIntermediateData() {
    }

    public static void setSuccess(ItemStack stack) {
        stack.getOrCreateTagElement(ROOT).putInt(RATING, SUCCESS_RATING);
    }

    public static boolean hasSuccess(ItemStack stack) {
        CompoundTag data = stack.getTagElement(ROOT);
        return data != null && data.getInt(RATING) == SUCCESS_RATING;
    }

    public static void setSource(ItemStack stack, Item source) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(source);
        stack.getOrCreateTagElement(ROOT).putString(SOURCE, id.toString());
        setSuccess(stack);
    }

    public static void copySource(ItemStack input, ItemStack output) {
        CompoundTag data = input.getTagElement(ROOT);
        if (data != null && data.contains(SOURCE)) {
            output.getOrCreateTagElement(ROOT).putString(SOURCE, data.getString(SOURCE));
        }
        setSuccess(output);
    }

    public static Optional<ResourceLocation> getSource(ItemStack stack) {
        CompoundTag data = stack.getTagElement(ROOT);
        if (data == null || !data.contains(SOURCE)) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(data.getString(SOURCE)));
    }

    public static Optional<Component> sourceName(ItemStack stack) {
        return getSource(stack)
                .map(BuiltInRegistries.ITEM::get)
                .filter(item -> item != net.minecraft.world.item.Items.AIR)
                .map(item -> new ItemStack(item).getHoverName());
    }

    public static boolean sameSource(ItemStack first, ItemStack second) {
        return getSource(first).equals(getSource(second));
    }
}
