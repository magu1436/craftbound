package com.magu1436.craftbound.occupations.foodproducer.processing;

import javax.annotation.Nullable;

import com.magu1436.craftbound.Craftbound;

import net.minecraft.network.FriendlyByteBuf;
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

/** 初期加工設備に共通する手動操作メニュー。 */
public final class FoodProcessingMenu extends AbstractContainerMenu {

    public static final int SELECT_CUT_BUTTON = 0;
    public static final int SELECT_MIX_BUTTON = 1;
    public static final int START_BUTTON = 10;

    private final Container container;
    private final ContainerData data;
    @Nullable
    private final FoodProcessingBlockEntity processor;

    public FoodProcessingMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, new SimpleContainer(FoodProcessingBlockEntity.CONTAINER_SIZE),
                new SimpleContainerData(FoodProcessingBlockEntity.DATA_COUNT), null);
        extraData.readBlockPos();
    }

    public FoodProcessingMenu(
            int containerId,
            Inventory inventory,
            FoodProcessingBlockEntity processor,
            ContainerData data
    ) {
        this(containerId, inventory, processor, data, processor);
    }

    private FoodProcessingMenu(
            int containerId,
            Inventory inventory,
            Container container,
            ContainerData data,
            @Nullable FoodProcessingBlockEntity processor
    ) {
        super(Craftbound.FOOD_PROCESSING_MENU.get(), containerId);
        checkContainerSize(container, FoodProcessingBlockEntity.CONTAINER_SIZE);
        checkContainerDataCount(data, FoodProcessingBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.processor = processor;

        for (int input = 0; input < 3; input++) {
            addSlot(new ProcessingSlot(container, input, 26 + input * 18, 36, processor));
        }
        addSlot(new ProcessingSlot(container, FoodProcessingBlockEntity.TOOL, 44, 61, processor) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Craftbound.COOKING_KNIFE.get()) && super.mayPlace(stack);
            }
        });
        addSlot(new OutputSlot(container, FoodProcessingBlockEntity.OUTPUT, 116, 36));
        addSlot(new OutputSlot(container, FoodProcessingBlockEntity.RETURN, 134, 36));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 105 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 163));
        }
        addDataSlots(data);
        container.startOpen(inventory.player);
    }

    public FoodProcessingStation station() {
        FoodProcessingStation[] values = FoodProcessingStation.values();
        int ordinal = data.get(FoodProcessingBlockEntity.DATA_STATION);
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : FoodProcessingStation.COOKING_TABLE;
    }

    public FoodProcessingOperation operation() {
        FoodProcessingOperation[] values = FoodProcessingOperation.values();
        int ordinal = data.get(FoodProcessingBlockEntity.DATA_OPERATION);
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : FoodProcessingOperation.CUT;
    }

    public int progress() {
        return data.get(FoodProcessingBlockEntity.DATA_PROGRESS);
    }

    public int totalTicks() {
        return data.get(FoodProcessingBlockEntity.DATA_TOTAL);
    }

    public boolean running() {
        return data.get(FoodProcessingBlockEntity.DATA_RUNNING) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (!(player instanceof ServerPlayer serverPlayer) || processor == null) return false;
        boolean changed = switch (buttonId) {
            case SELECT_CUT_BUTTON -> processor.selectOperation(FoodProcessingOperation.CUT);
            case SELECT_MIX_BUTTON -> processor.selectOperation(FoodProcessingOperation.MIX);
            case START_BUTTON -> processor.start(serverPlayer);
            default -> false;
        };
        if (changed) broadcastChanges();
        return changed;
    }

    @Override
    public boolean stillValid(Player player) {
        return processor == null || processor.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int machineSlots = FoodProcessingBlockEntity.CONTAINER_SIZE;

        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.is(Craftbound.COOKING_KNIFE.get())) {
            if (!moveItemStackTo(stack, FoodProcessingBlockEntity.TOOL,
                    FoodProcessingBlockEntity.TOOL + 1, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, FoodProcessingBlockEntity.INPUT_0,
                FoodProcessingBlockEntity.INPUT_2 + 1, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        container.setChanged();
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    private static class ProcessingSlot extends Slot {
        @Nullable
        private final FoodProcessingBlockEntity processor;

        ProcessingSlot(Container container, int slot, int x, int y,
                @Nullable FoodProcessingBlockEntity processor) {
            super(container, slot, x, y);
            this.processor = processor;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return processor == null || !processor.slotLocked(getSlotIndex());
        }

        @Override
        public boolean mayPickup(Player player) {
            return processor == null || !processor.slotLocked(getSlotIndex());
        }
    }

    private static final class OutputSlot extends Slot {
        OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
