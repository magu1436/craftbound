package com.magu1436.craftbound.occupations.foodproducer.ranch;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.registry.CraftboundBlockEntities;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityData;
import com.magu1436.craftbound.occupations.foodproducer.skills.FoodProducerSkills;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
    public static final int DATA_RANGE_COUNT = 9;
    public static final int DATA_REGISTERED_COUNT = 10;
    public static final int DATA_OVERFLOW_COUNT = 11;
    public static final int DATA_COUNT = 12;

    public static final long OUTSIDE_GRACE_TICKS = 30L * 20L;
    private static final long DISCOVERY_INTERVAL_TICKS = 5L * 20L;

    private static final String TARGET_TAG = "target";
    private static final String FEED_RANK_TAG = "feed_management_rank";
    private static final String CAPACITY_RANK_TAG = "ranch_capacity_rank";
    private static final String REGISTRATIONS_TAG = "registered_animals";
    private static final String REGISTRATION_UUID_TAG = "animal_uuid";
    private static final String OUTSIDE_SINCE_TAG = "outside_since";
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
    private int rangeCount;
    private int overflowCount;
    private long lastDiscoveryGameTime = Long.MIN_VALUE;
    private final RanchRegistrationRoster registrations = new RanchRegistrationRoster();
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
                case DATA_RANGE_COUNT -> rangeCount;
                case DATA_REGISTERED_COUNT -> registrations.size();
                case DATA_OVERFLOW_COUNT -> overflowCount;
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
                case DATA_RANGE_COUNT -> rangeCount = Math.max(0, value);
                case DATA_OVERFLOW_COUNT -> overflowCount = Math.max(0, value);
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
        super(CraftboundBlockEntities.RANCH_BLOCK.get(), pos, state);
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
        if (target == newTarget) {
            return true;
        }
        releaseAllAssignments();
        target = newTarget;
        lastDiscoveryGameTime = Long.MIN_VALUE;
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
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        reconcileRegistrations(serverLevel, gameTime);

        List<Animal> animalsInRange = null;
        if (lastDiscoveryGameTime == Long.MIN_VALUE
                || gameTime - lastDiscoveryGameTime >= DISCOVERY_INTERVAL_TICKS) {
            lastDiscoveryGameTime = gameTime;
            animalsInRange = RanchManager.getAnimalsInRange(this);
            animalsInRange.stream()
                    .filter(Animal::isBaby)
                    .forEach(RanchAnimalData::ensureManaged);
            fillAvailableRegistrations(serverLevel, animalsInRange);
            rangeCount = animalsInRange.size();
        }

        List<Animal> managed = getActiveManagedAnimals();
        managedCount = managed.size();
        if (animalsInRange != null) {
            overflowCount = Math.max(0, rangeCount - managedCount);
        }
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

    private void reconcileRegistrations(ServerLevel serverLevel, long gameTime) {
        if (target == RanchTarget.UNSET) {
            releaseAllAssignments();
            return;
        }

        for (Map.Entry<UUID, Long> entry : registrations.entries()) {
            UUID animalId = entry.getKey();
            Animal animal = RanchManager.findLoadedAnimal(this, animalId);
            if (animal == null) {
                if (registrations.graceExpired(animalId, gameTime, OUTSIDE_GRACE_TICKS)) {
                    releaseRegistration(animalId);
                }
                continue;
            }
            if (!animal.isAlive() || !target.matches(animal)) {
                releaseRegistration(animalId);
                continue;
            }

            RanchBlockEntity assigned = RanchManager.findAssignedRanch(animal);
            if (assigned != null && assigned != this) {
                releaseRegistration(animalId);
                continue;
            }
            if (animal.level() == serverLevel && getManagementBounds().contains(animal.position())) {
                registrations.markInside(animalId);
                RanchAnimalData.assignTo(animal, serverLevel, worldPosition);
            } else {
                registrations.markOutside(animalId, gameTime);
                if (registrations.graceExpired(animalId, gameTime, OUTSIDE_GRACE_TICKS)) {
                    releaseRegistration(animalId);
                }
            }
        }
    }

    private void fillAvailableRegistrations(ServerLevel serverLevel, List<Animal> candidates) {
        if (!hasRegistrationSpace()) {
            return;
        }
        for (Animal animal : candidates) {
            if (!hasRegistrationSpace()) {
                break;
            }
            if (!registrations.contains(animal.getUUID()) && RanchManager.mayRegister(animal, this)) {
                registerAnimal(serverLevel, animal);
            }
        }
    }

    private boolean registerAnimal(ServerLevel serverLevel, Animal animal) {
        if (!target.matches(animal)
                || !registrations.register(animal.getUUID(), getManagementCapacity())) {
            return false;
        }
        RanchAnimalData.assignTo(animal, serverLevel, worldPosition);
        RanchAnimalData.ensureManaged(animal);
        setChanged();
        return true;
    }

    public boolean registerNewborn(Animal animal) {
        return level instanceof ServerLevel serverLevel && registerAnimal(serverLevel, animal);
    }

    public boolean hasRegistration(UUID animalId) {
        return registrations.contains(animalId);
    }

    public boolean hasRegistrationSpace() {
        return target != RanchTarget.UNSET && registrations.size() < getManagementCapacity();
    }

    public int getRegistrationCount() {
        return registrations.size();
    }

    public int getRangeCount() {
        return rangeCount;
    }

    public int getOverflowCount() {
        return overflowCount;
    }

    public int getOutsideGraceSeconds(UUID animalId, long gameTime) {
        long outsideSince = registrations.outsideSince(animalId);
        if (!registrations.contains(animalId) || outsideSince == RanchRegistrationRoster.INSIDE) {
            return -1;
        }
        long remaining = Math.max(0L, OUTSIDE_GRACE_TICKS - (gameTime - outsideSince));
        return (int) ((remaining + 19L) / 20L);
    }

    List<Animal> getActiveManagedAnimals() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return List.of();
        }
        return registrations.ids().stream()
                .map(id -> RanchManager.findLoadedAnimal(this, id))
                .filter(java.util.Objects::nonNull)
                .filter(Animal::isAlive)
                .filter(animal -> animal.level() == serverLevel)
                .filter(target::matches)
                .filter(animal -> getManagementBounds().contains(animal.position()))
                .filter(animal -> RanchAnimalData.isAssignedTo(animal, serverLevel, worldPosition))
                .toList();
    }

    public void releaseRegistration(UUID animalId) {
        Animal animal = RanchManager.findLoadedAnimal(this, animalId);
        if (animal != null && level instanceof ServerLevel serverLevel) {
            RanchAnimalData.clearAssignmentIfMatches(animal, serverLevel, worldPosition);
        }
        if (registrations.release(animalId)) {
            lastDiscoveryGameTime = Long.MIN_VALUE;
            setChanged();
        }
    }

    public void releaseAllAssignments() {
        if (level instanceof ServerLevel serverLevel) {
            for (UUID animalId : registrations.ids()) {
                Animal animal = RanchManager.findLoadedAnimal(this, animalId);
                if (animal != null) {
                    RanchAnimalData.clearAssignmentIfMatches(animal, serverLevel, worldPosition);
                }
            }
        }
        if (registrations.size() > 0) {
            registrations.clear();
            managedCount = 0;
            rangeCount = 0;
            overflowCount = 0;
            setChanged();
        }
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
        lastDiscoveryGameTime = Long.MIN_VALUE;
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
        ListTag registrationList = new ListTag();
        for (Map.Entry<UUID, Long> entry : registrations.entries()) {
            CompoundTag registration = new CompoundTag();
            registration.putUUID(REGISTRATION_UUID_TAG, entry.getKey());
            registration.putLong(OUTSIDE_SINCE_TAG, entry.getValue());
            registrationList.add(registration);
        }
        tag.put(REGISTRATIONS_TAG, registrationList);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        target = RanchTarget.fromSerializedName(tag.getString(TARGET_TAG));
        feedManagementRank = clampRank(tag.getInt(FEED_RANK_TAG));
        ranchCapacityRank = clampRank(tag.getInt(CAPACITY_RANK_TAG));
        registrations.clear();
        ListTag registrationList = tag.getList(REGISTRATIONS_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < registrationList.size(); index++) {
            CompoundTag registration = registrationList.getCompound(index);
            if (registration.hasUUID(REGISTRATION_UUID_TAG)
                    && registrations.size() < getManagementCapacity()) {
                registrations.load(
                        registration.getUUID(REGISTRATION_UUID_TAG),
                        registration.getLong(OUTSIDE_SINCE_TAG)
                );
            }
        }
        lastDiscoveryGameTime = Long.MIN_VALUE;
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
