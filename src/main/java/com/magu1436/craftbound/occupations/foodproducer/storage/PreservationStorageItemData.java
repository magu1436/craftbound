package com.magu1436.craftbound.occupations.foodproducer.storage;

import java.util.function.Consumer;
import java.util.function.Function;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.registry.CraftboundItems;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityData;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityItems;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

/** アイテム化した保存設備の内部在庫を1倍の品質時計で管理する。 */
public final class PreservationStorageItemData {

    private static final String BLOCK_ENTITY_TAG = "BlockEntityTag";
    private static final String ITEMS_TAG = "Items";
    private static final double ITEMIZED_MULTIPLIER = 1.0D;

    private PreservationStorageItemData() {
    }

    public static boolean isStorageItem(ItemStack stack) {
        return stack.is(CraftboundItems.PRESERVATION_STORAGE_1.get())
                || stack.is(CraftboundItems.PRESERVATION_STORAGE_2.get());
    }

    public static boolean advanceLoadedTime(ItemStack stack, long gameTime) {
        return update(stack, item -> FoodQualityData.advanceLoadedTime(
                item,
                gameTime,
                ITEMIZED_MULTIPLIER
        ));
    }

    public static boolean pauseClock(ItemStack stack, long gameTime) {
        return update(stack, item -> FoodQualityData.pauseClock(
                item,
                gameTime,
                ITEMIZED_MULTIPLIER
        ));
    }

    public static boolean resetClock(ItemStack stack, long gameTime) {
        return visit(stack, item -> FoodQualityData.resetClock(
                item,
                gameTime,
                ITEMIZED_MULTIPLIER
        ));
    }

    public static int storedItemCount(ItemStack stack) {
        CompoundTag blockEntityTag = blockEntityTag(stack);
        if (blockEntityTag == null) {
            return 0;
        }
        NonNullList<ItemStack> items = loadItems(blockEntityTag);
        return items.stream().mapToInt(ItemStack::getCount).sum();
    }

    private static boolean update(ItemStack storage, Function<ItemStack, Boolean> action) {
        CompoundTag blockEntityTag = blockEntityTag(storage);
        if (blockEntityTag == null) {
            return false;
        }

        NonNullList<ItemStack> items = loadItems(blockEntityTag);
        boolean changed = false;
        for (ItemStack item : items) {
            if (FoodQualityItems.isQualityTarget(item)) {
                changed |= action.apply(item);
            }
        }
        if (changed) {
            saveItems(storage, blockEntityTag, items);
        }
        return changed;
    }

    private static boolean visit(ItemStack storage, Consumer<ItemStack> action) {
        CompoundTag blockEntityTag = blockEntityTag(storage);
        if (blockEntityTag == null) {
            return false;
        }

        NonNullList<ItemStack> items = loadItems(blockEntityTag);
        boolean found = false;
        for (ItemStack item : items) {
            if (FoodQualityItems.isQualityTarget(item)) {
                action.accept(item);
                found = true;
            }
        }
        if (found) {
            saveItems(storage, blockEntityTag, items);
        }
        return found;
    }

    private static CompoundTag blockEntityTag(ItemStack storage) {
        if (!isStorageItem(storage)) {
            return null;
        }
        CompoundTag root = storage.getTag();
        if (root == null || !root.contains(BLOCK_ENTITY_TAG, Tag.TAG_COMPOUND)) {
            return null;
        }
        CompoundTag blockEntityTag = root.getCompound(BLOCK_ENTITY_TAG);
        return blockEntityTag.contains(ITEMS_TAG, Tag.TAG_LIST) ? blockEntityTag : null;
    }

    private static NonNullList<ItemStack> loadItems(CompoundTag blockEntityTag) {
        NonNullList<ItemStack> items = NonNullList.withSize(
                PreservationStorageBlockEntity.CONTAINER_SIZE,
                ItemStack.EMPTY
        );
        ContainerHelper.loadAllItems(blockEntityTag, items);
        return items;
    }

    private static void saveItems(
            ItemStack storage,
            CompoundTag blockEntityTag,
            NonNullList<ItemStack> items
    ) {
        ContainerHelper.saveAllItems(blockEntityTag, items);
        storage.getOrCreateTag().put(BLOCK_ENTITY_TAG, blockEntityTag);
    }
}
