package com.magu1436.craftbound.occupations.foodproducer.ranch;

import javax.annotation.Nullable;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.registry.CraftboundMenus;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityData;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** 牧畜ブロックの餌1枠と対象動物設定を同期するメニュー。 */
public final class RanchMenu extends AbstractContainerMenu {

    public static final int UPDATE_PERFORMANCE_BUTTON = 100;
    private static final int FEED_SLOT_X = 80;
    private static final int FEED_SLOT_Y = 44;
    private static final int PLAYER_INVENTORY_Y = 113;
    private static final int HOTBAR_Y = 171;

    private final Container ranchContainer;
    private final ContainerData data;
    @Nullable
    private final RanchBlockEntity ranchBlockEntity;

    public RanchMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(
                containerId,
                inventory,
                new SimpleContainer(RanchBlockEntity.CONTAINER_SIZE),
                new SimpleContainerData(RanchBlockEntity.DATA_COUNT),
                null
        );
        extraData.readBlockPos();
    }

    public RanchMenu(
            int containerId,
            Inventory inventory,
            RanchBlockEntity ranchBlockEntity,
            ContainerData data
    ) {
        this(containerId, inventory, ranchBlockEntity, data, ranchBlockEntity);
    }

    private RanchMenu(
            int containerId,
            Inventory inventory,
            Container ranchContainer,
            ContainerData data,
            @Nullable RanchBlockEntity ranchBlockEntity
    ) {
        super(CraftboundMenus.RANCH_BLOCK.get(), containerId);
        checkContainerSize(ranchContainer, RanchBlockEntity.CONTAINER_SIZE);
        checkContainerDataCount(data, RanchBlockEntity.DATA_COUNT);
        this.ranchContainer = ranchContainer;
        this.data = data;
        this.ranchBlockEntity = ranchBlockEntity;

        addSlot(new Slot(ranchContainer, RanchBlockEntity.FEED_SLOT, FEED_SLOT_X, FEED_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return getTarget().accepts(stack) && !FoodQualityData.isSpoiled(stack);
            }
        });

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
        ranchContainer.startOpen(inventory.player);
    }

    public RanchTarget getTarget() {
        return RanchTarget.fromId(data.get(RanchBlockEntity.DATA_TARGET));
    }

    public int getFeedManagementRank() {
        return data.get(RanchBlockEntity.DATA_FEED_RANK);
    }

    public int getRanchCapacityRank() {
        return data.get(RanchBlockEntity.DATA_CAPACITY_RANK);
    }

    public int getManagementCapacity() {
        return data.get(RanchBlockEntity.DATA_CAPACITY);
    }

    public int getManagedCount() {
        return data.get(RanchBlockEntity.DATA_MANAGED_COUNT);
    }

    public int getAdultCount() {
        return data.get(RanchBlockEntity.DATA_ADULT_COUNT);
    }

    public int getChildCount() {
        return data.get(RanchBlockEntity.DATA_CHILD_COUNT);
    }

    public int getBreedableCount() {
        return data.get(RanchBlockEntity.DATA_BREEDABLE_COUNT);
    }

    public int getNextFeedSeconds() {
        return data.get(RanchBlockEntity.DATA_NEXT_FEED_SECONDS);
    }

    public boolean canChangeTarget() {
        return !getSlot(RanchBlockEntity.FEED_SLOT).hasItem();
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (ranchBlockEntity == null) {
            return false;
        }
        if (buttonId == UPDATE_PERFORMANCE_BUTTON && player instanceof ServerPlayer serverPlayer) {
            int result = ranchBlockEntity.updatePerformanceRanks(serverPlayer);
            String key = result < 0
                    ? "message.craftbound.ranch.update_requires_skill"
                    : result == 0
                            ? "message.craftbound.ranch.update_unchanged"
                            : "message.craftbound.ranch.update_success";
            serverPlayer.displayClientMessage(Component.translatable(key), true);
            broadcastChanges();
            return result >= 0;
        }
        if (!canChangeTarget()) {
            return false;
        }
        RanchTarget requested = RanchTarget.fromId(buttonId);
        if (!ranchBlockEntity.setTarget(requested)) {
            return false;
        }
        broadcastChanges();
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return ranchBlockEntity == null || ranchBlockEntity.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == RanchBlockEntity.FEED_SLOT) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (getTarget().accepts(stack)) {
            if (!moveItemStackTo(stack, RanchBlockEntity.FEED_SLOT, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        ranchContainer.setChanged();
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        ranchContainer.stopOpen(player);
    }
}
