package com.magu1436.craftbound.occupations.foodproducer.storage;

import java.util.stream.IntStream;

import javax.annotation.Nullable;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityData;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;

/** 品質対象だけを27スロットで保管し、設置ブロックに応じた保存倍率を適用する。 */
public final class PreservationStorageBlockEntity extends BaseContainerBlockEntity
        implements WorldlyContainer, PreservationMultiplierContainer {

    public static final int CONTAINER_SIZE = 27;
    public static final int DATA_MULTIPLIER = 0;
    public static final int DATA_COUNT = 1;

    private static final int[] ALL_SLOTS = IntStream.range(0, CONTAINER_SIZE).toArray();

    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private LazyOptional<IItemHandler> itemHandler = LazyOptional.of(this::createItemHandler);

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return index == DATA_MULTIPLIER ? (int) preservationMultiplier() : 0;
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public PreservationStorageBlockEntity(BlockPos pos, BlockState state) {
        super(Craftbound.PRESERVATION_STORAGE_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            boolean changed = advanceAll(level.getGameTime(), preservationMultiplier());
            if (changed) {
                setChanged();
            }
        }
    }

    @Override
    public double preservationMultiplier() {
        return getBlockState().getBlock() instanceof PreservationStorageBlock storage
                ? storage.preservationMultiplier()
                : 1.0D;
    }

    public ContainerData dataAccess() {
        return dataAccess;
    }

    public void saveToItem(ItemStack stack) {
        settleAndPauseContents();
        CompoundTag blockEntityTag = saveWithoutMetadata();
        BlockItem.setBlockEntityData(
                stack,
                Craftbound.PRESERVATION_STORAGE_BLOCK_ENTITY.get(),
                blockEntityTag
        );
        if (hasCustomName()) {
            stack.setHoverName(getCustomName());
        }
    }

    public void settleAndPauseContents() {
        if (level == null || level.isClientSide) {
            return;
        }
        long gameTime = level.getGameTime();
        double multiplier = preservationMultiplier();
        boolean changed = false;
        for (ItemStack stack : items) {
            if (FoodQualityItems.isQualityTarget(stack)) {
                changed |= FoodQualityData.pauseClock(stack, gameTime, multiplier);
            }
        }
        if (changed) {
            setChanged();
        }
    }

    public boolean insertOneFromHopper(ItemStack source) {
        if (source.isEmpty() || !FoodQualityItems.isQualityTarget(source)) {
            return false;
        }
        ItemStack one = source.copy();
        one.setCount(1);
        IItemHandler handler = createItemHandler();
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) {
            ItemStack remainder = handler.insertItem(slot, one, false);
            if (remainder.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected Component getDefaultName() {
        String key = getBlockState().is(Craftbound.PRESERVATION_STORAGE_2.get())
                ? "container.craftbound.preservation_storage_2"
                : "container.craftbound.preservation_storage_1";
        return Component.translatable(key);
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new PreservationStorageMenu(containerId, inventory, this, dataAccess);
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        settleSlot(slot);
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        settleSlot(slot);
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!stack.isEmpty() && level != null && !level.isClientSide) {
            FoodQualityData.advanceLoadedTime(
                    stack,
                    level.getGameTime(),
                    preservationMultiplier()
            );
        }
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D
        ) <= 64.0D;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= 0 && slot < CONTAINER_SIZE && FoodQualityItems.isQualityTarget(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= 0 && slot < CONTAINER_SIZE;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemHandler = LazyOptional.of(this::createItemHandler);
    }

    private boolean advanceAll(long gameTime, double multiplier) {
        boolean changed = false;
        for (ItemStack stack : items) {
            if (FoodQualityItems.isQualityTarget(stack)) {
                changed |= FoodQualityData.advanceLoadedTime(stack, gameTime, multiplier);
            }
        }
        return changed;
    }

    private void settleSlot(int slot) {
        if (level != null && !level.isClientSide && slot >= 0 && slot < CONTAINER_SIZE) {
            FoodQualityData.advanceLoadedTime(
                    items.get(slot),
                    level.getGameTime(),
                    preservationMultiplier()
            );
        }
    }

    private IItemHandler createItemHandler() {
        return new StorageItemHandler(this);
    }

    private final class StorageItemHandler extends InvWrapper {

        private StorageItemHandler(PreservationStorageBlockEntity storage) {
            super(storage);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return canPlaceItem(slot, stack);
        }

        @Override
        @NotNull
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !isItemValid(slot, stack)) {
                return stack;
            }

            ItemStack stored = items.get(slot);
            int limit = Math.min(getSlotLimit(slot), stack.getMaxStackSize());
            int space = stored.isEmpty() ? limit : limit - stored.getCount();
            if (space <= 0) {
                return stack;
            }

            long gameTime = level == null ? 0L : level.getGameTime();
            double multiplier = preservationMultiplier();
            ItemStack prepared = stack.copy();
            if (stored.isEmpty()) {
                FoodQualityData.advanceLoadedTime(prepared, gameTime, multiplier);
            } else {
                ItemStack storedForCheck = simulate ? stored.copy() : stored;
                if (!FoodQualityData.prepareForMerge(
                        storedForCheck,
                        prepared,
                        gameTime,
                        multiplier
                )) {
                    return stack;
                }
            }

            int inserted = Math.min(space, stack.getCount());
            if (!simulate) {
                ItemStack merged = prepared.copy();
                merged.setCount(inserted + (stored.isEmpty() ? 0 : stored.getCount()));
                setItem(slot, merged);
            }

            if (inserted >= stack.getCount()) {
                return ItemStack.EMPTY;
            }
            ItemStack remainder = stack.copy();
            remainder.shrink(inserted);
            return remainder;
        }

        @Override
        @NotNull
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            settleSlot(slot);
            return super.extractItem(slot, amount, simulate);
        }
    }
}
