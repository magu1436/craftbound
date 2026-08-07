package com.magu1436.craftbound.occupations.foodproducer.ranch;

import javax.annotation.Nullable;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityData;
import com.magu1436.craftbound.occupations.foodproducer.skills.FoodProducerSkills;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;

/** 対象動物、餌1枠、および設置時の牧畜スキルランクを永続化する。 */
public final class RanchBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    public static final int FEED_SLOT = 0;
    public static final int CONTAINER_SIZE = 1;
    public static final int DATA_TARGET = 0;
    public static final int DATA_FEED_RANK = 1;
    public static final int DATA_CAPACITY_RANK = 2;
    public static final int DATA_CAPACITY = 3;
    public static final int DATA_MANAGED_COUNT = 4;
    public static final int DATA_ADULT_COUNT = 5;
    public static final int DATA_CHILD_COUNT = 6;
    public static final int DATA_BREEDABLE_COUNT = 7;
    public static final int DATA_NEXT_FEED_SECONDS = 8;
    public static final int DATA_COUNT = 9;

    private static final String TARGET_TAG = "target";
    private static final String FEED_RANK_TAG = "feed_management_rank";
    private static final String CAPACITY_RANK_TAG = "ranch_capacity_rank";
    private static final int[] ALL_SLOTS = { FEED_SLOT };

    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private RanchTarget target = RanchTarget.UNSET;
    private int feedManagementRank;
    private int ranchCapacityRank;
    private int managedCount;
    private int adultCount;
    private int childCount;
    private int breedableCount;
    private int nextFeedSeconds = -1;
    private LazyOptional<IItemHandler> itemHandler = LazyOptional.of(this::createItemHandler);

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_TARGET -> target.id();
                case DATA_FEED_RANK -> feedManagementRank;
                case DATA_CAPACITY_RANK -> ranchCapacityRank;
                case DATA_CAPACITY -> getManagementCapacity();
                case DATA_MANAGED_COUNT -> managedCount;
                case DATA_ADULT_COUNT -> adultCount;
                case DATA_CHILD_COUNT -> childCount;
                case DATA_BREEDABLE_COUNT -> breedableCount;
                case DATA_NEXT_FEED_SECONDS -> nextFeedSeconds;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_TARGET -> target = RanchTarget.fromId(value);
                case DATA_FEED_RANK -> feedManagementRank = clampRank(value);
                case DATA_CAPACITY_RANK -> ranchCapacityRank = clampRank(value);
                case DATA_MANAGED_COUNT -> managedCount = Math.max(0, value);
                case DATA_ADULT_COUNT -> adultCount = Math.max(0, value);
                case DATA_CHILD_COUNT -> childCount = Math.max(0, value);
                case DATA_BREEDABLE_COUNT -> breedableCount = Math.max(0, value);
                case DATA_NEXT_FEED_SECONDS -> nextFeedSeconds = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public RanchBlockEntity(BlockPos pos, BlockState state) {
        super(Craftbound.RANCH_BLOCK_ENTITY.get(), pos, state);
    }

    public void initializePlacementRanks(@Nullable ServerPlayer placer) {
        if (placer == null) {
            feedManagementRank = 0;
            ranchCapacityRank = 0;
        } else {
            feedManagementRank = clampRank(FoodProducerSkills.rank(
                    placer,
                    FoodProducerSkills.FEED_MANAGEMENT
            ));
            ranchCapacityRank = clampRank(FoodProducerSkills.rank(
                    placer,
                    FoodProducerSkills.RANCH_CAPACITY
            ));
        }
        setChanged();
    }

    public RanchTarget getTarget() {
        return target;
    }

    public boolean setTarget(RanchTarget newTarget) {
        if (!items.get(FEED_SLOT).isEmpty()) {
            return false;
        }
        target = newTarget;
        setChanged();
        return true;
    }

    /**
     * ホッパーから餌を1個搬入する。
     * 品質時計だけが異なる同品質品は、短い方の残り時間へ揃えて統合する。
     */
    public boolean insertOneFromHopper(ItemStack sourceStack, long gameTime) {
        if (sourceStack.isEmpty() || !canPlaceItem(FEED_SLOT, sourceStack)) {
            return false;
        }

        ItemStack incoming = sourceStack.copy();
        incoming.setCount(1);
        FoodQualityData.advanceLoadedTime(incoming, gameTime, 1.0D);

        ItemStack stored = items.get(FEED_SLOT);
        if (stored.isEmpty()) {
            items.set(FEED_SLOT, incoming);
            setChanged();
            return true;
        }

        int limit = Math.min(getMaxStackSize(), stored.getMaxStackSize());
        if (stored.getCount() >= limit
                || !FoodQualityData.prepareForMerge(stored, incoming, gameTime, 1.0D)) {
            return false;
        }

        stored.grow(1);
        setChanged();
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RanchBlockEntity ranch) {
        if (level.isClientSide || level.getGameTime() % 20L != 0L) {
            return;
        }

        ItemStack feed = ranch.items.get(FEED_SLOT);
        if (!feed.isEmpty() && FoodQualityData.advanceLoadedTime(
                feed,
                level.getGameTime(),
                1.0D
        )) {
            ranch.setChanged();
        }
        ranch.processManagedAnimals(level.getGameTime());
    }

    private void processManagedAnimals(long gameTime) {
        java.util.List<Animal> owned = RanchManager.getOwnedAnimals(this);
        owned.stream().filter(Animal::isBaby).forEach(RanchAnimalData::ensureManaged);
        java.util.List<Animal> managed = owned.stream().limit(getManagementCapacity()).toList();
        managedCount = managed.size();
        adultCount = 0;
        childCount = 0;
        breedableCount = 0;
        nextFeedSeconds = -1;

        for (Animal animal : managed) {
            RanchAnimalData.ensureManaged(animal);
            updateFeeding(animal, gameTime);

            if (animal.isBaby()) {
                childCount++;
                if (RanchAnimalData.isFed(animal)) {
                    int remaining = RanchAnimalData.getRemainingGrowth(animal);
                    RanchAnimalData.setRemainingGrowth(animal, Math.max(0, remaining - 20));
                }
            } else {
                adultCount++;
                if (animal.getAge() == 0 && RanchAnimalData.isFed(animal)) {
                    breedableCount++;
                }
            }

            if (RanchAnimalData.hasNextFeedTime(animal)) {
                long remainingTicks = Math.max(0L, RanchAnimalData.getNextFeedTime(animal) - gameTime);
                int seconds = (int) Math.min(Integer.MAX_VALUE, (remainingTicks + 19L) / 20L);
                nextFeedSeconds = nextFeedSeconds < 0 ? seconds : Math.min(nextFeedSeconds, seconds);
            }
        }
        setChanged();
    }

    private void updateFeeding(Animal animal, long gameTime) {
        if (RanchAnimalData.hasNextFeedTime(animal)
                && gameTime < RanchAnimalData.getNextFeedTime(animal)) {
            return;
        }

        ItemStack feed = items.get(FEED_SLOT);
        if (feed.isEmpty() || FoodQualityData.isSpoiled(feed) || !target.accepts(feed)) {
            RanchAnimalData.setFed(animal, false);
            RanchAnimalData.setNextFeedTime(animal, gameTime);
            return;
        }

        boolean saved = level != null && level.random.nextInt(100) < feedManagementRank * 10;
        if (!saved) {
            feed.shrink(1);
            if (feed.isEmpty()) {
                items.set(FEED_SLOT, ItemStack.EMPTY);
            }
            setChanged();
        }
        RanchAnimalData.setFed(animal, true);
        RanchAnimalData.setNextFeedTime(
                animal,
                gameTime + (animal.isBaby()
                        ? RanchAnimalData.CHILD_FEED_INTERVAL_TICKS
                        : RanchAnimalData.ADULT_FEED_INTERVAL_TICKS)
        );
    }

    public int getFeedManagementRank() {
        return feedManagementRank;
    }

    public int getRanchCapacityRank() {
        return ranchCapacityRank;
    }

    public int getManagementCapacity() {
        return switch (ranchCapacityRank) {
            case 1 -> 12;
            case 2 -> 16;
            case 3 -> 20;
            default -> 8;
        };
    }

    /** 牧畜管理取得者の現在ランクで性能を更新する。既存値は低下させない。 */
    public int updatePerformanceRanks(ServerPlayer player) {
        if (!FoodProducerSkills.has(player, FoodProducerSkills.RANCH_MANAGEMENT)) {
            return -1;
        }
        int newFeedRank = Math.max(feedManagementRank,
                clampRank(FoodProducerSkills.rank(player, FoodProducerSkills.FEED_MANAGEMENT)));
        int newCapacityRank = Math.max(ranchCapacityRank,
                clampRank(FoodProducerSkills.rank(player, FoodProducerSkills.RANCH_CAPACITY)));
        if (newFeedRank == feedManagementRank && newCapacityRank == ranchCapacityRank) {
            return 0;
        }
        feedManagementRank = newFeedRank;
        ranchCapacityRank = newCapacityRank;
        setChanged();
        return 1;
    }

    /** 仕様で確定した、境界を含む16×16×33の管理範囲。 */
    public AABB getManagementBounds() {
        return new AABB(
                worldPosition.getX() - 7,
                worldPosition.getY() - 16,
                worldPosition.getZ() - 7,
                worldPosition.getX() + 9,
                worldPosition.getY() + 17,
                worldPosition.getZ() + 9
        );
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.craftbound.ranch_block");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new RanchMenu(containerId, inventory, this, dataAccess);
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
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
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
        return slot == FEED_SLOT
                && target.accepts(stack)
                && !FoodQualityData.isSpoiled(stack);
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
        return slot == FEED_SLOT;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putString(TARGET_TAG, target.serializedName());
        tag.putInt(FEED_RANK_TAG, feedManagementRank);
        tag.putInt(CAPACITY_RANK_TAG, ranchCapacityRank);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        target = RanchTarget.fromSerializedName(tag.getString(TARGET_TAG));
        feedManagementRank = clampRank(tag.getInt(FEED_RANK_TAG));
        ranchCapacityRank = clampRank(tag.getInt(CAPACITY_RANK_TAG));
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

    private IItemHandler createItemHandler() {
        return new RanchItemHandler(this);
    }

    private final class RanchItemHandler extends InvWrapper {

        private RanchItemHandler(RanchBlockEntity ranch) {
            super(ranch);
        }

        @Override
        @NotNull
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot != FEED_SLOT || stack.isEmpty() || !canPlaceItem(slot, stack)) {
                return stack;
            }

            ItemStack stored = items.get(slot);
            int limit = Math.min(getSlotLimit(slot), stack.getMaxStackSize());
            int space = stored.isEmpty() ? limit : limit - stored.getCount();
            if (space <= 0) {
                return stack;
            }

            long gameTime = level == null ? 0L : level.getGameTime();
            ItemStack prepared = stack.copy();
            if (stored.isEmpty()) {
                FoodQualityData.advanceLoadedTime(prepared, gameTime, 1.0D);
            } else {
                ItemStack storedForCheck = simulate ? stored.copy() : stored;
                if (!FoodQualityData.prepareForMerge(storedForCheck, prepared, gameTime, 1.0D)) {
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
    }

    private static int clampRank(int rank) {
        return Math.max(0, Math.min(3, rank));
    }
}
