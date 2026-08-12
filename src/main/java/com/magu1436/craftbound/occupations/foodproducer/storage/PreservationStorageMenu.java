package com.magu1436.craftbound.occupations.foodproducer.storage;

import javax.annotation.Nullable;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.registry.CraftboundMenus;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityData;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityItems;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;

/** 保存設備の27スロットと保存倍率を同期するチェスト型メニュー。 */
public final class PreservationStorageMenu extends AbstractContainerMenu {

    private static final int STORAGE_ROWS = 3;
    private static final int STORAGE_SLOT_COUNT = STORAGE_ROWS * 9;
    private static final int PLAYER_INVENTORY_Y = 85;
    private static final int HOTBAR_Y = 143;

    private final Container storageContainer;
    private final ContainerData data;
    @Nullable
    private final PreservationStorageBlockEntity storageBlockEntity;

    public PreservationStorageMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(
                containerId,
                inventory,
                new SimpleContainer(PreservationStorageBlockEntity.CONTAINER_SIZE),
                new SimpleContainerData(PreservationStorageBlockEntity.DATA_COUNT),
                null
        );
        extraData.readBlockPos();
    }

    public PreservationStorageMenu(
            int containerId,
            Inventory inventory,
            PreservationStorageBlockEntity storage,
            ContainerData data
    ) {
        this(containerId, inventory, storage, data, storage);
    }

    private PreservationStorageMenu(
            int containerId,
            Inventory inventory,
            Container storageContainer,
            ContainerData data,
            @Nullable PreservationStorageBlockEntity storageBlockEntity
    ) {
        super(CraftboundMenus.PRESERVATION_STORAGE.get(), containerId);
        checkContainerSize(storageContainer, PreservationStorageBlockEntity.CONTAINER_SIZE);
        checkContainerDataCount(data, PreservationStorageBlockEntity.DATA_COUNT);
        this.storageContainer = storageContainer;
        this.data = data;
        this.storageBlockEntity = storageBlockEntity;

        for (int row = 0; row < STORAGE_ROWS; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(
                        storageContainer,
                        column + row * 9,
                        8 + column * 18,
                        18 + row * 18
                ) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return FoodQualityItems.isQualityTarget(stack);
                    }
                });
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(
                        inventory,
                        column + row * 9 + 9,
                        8 + column * 18,
                        PLAYER_INVENTORY_Y + row * 18
                ));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, HOTBAR_Y));
        }

        addDataSlots(data);
        storageContainer.startOpen(inventory.player);
    }

    public int preservationMultiplier() {
        return Math.max(1, data.get(PreservationStorageBlockEntity.DATA_MULTIPLIER));
    }

    public boolean isStorageSlot(Slot slot) {
        int menuIndex = slots.indexOf(slot);
        return menuIndex >= 0 && menuIndex < STORAGE_SLOT_COUNT;
    }

    @Override
    public boolean stillValid(Player player) {
        return storageBlockEntity == null || storageBlockEntity.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < STORAGE_SLOT_COUNT) {
            if (storageBlockEntity != null && player instanceof ServerPlayer serverPlayer) {
                FoodQualityData.advanceLoadedTime(
                        stack,
                        serverPlayer.serverLevel().getGameTime(),
                        storageBlockEntity.preservationMultiplier()
                );
            }
            if (!moveItemStackTo(stack, STORAGE_SLOT_COUNT, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!FoodQualityItems.isQualityTarget(stack)
                    || !moveItemStackTo(stack, 0, STORAGE_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        storageContainer.setChanged();
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        storageContainer.stopOpen(player);
    }
}
