package com.magu1436.craftbound.occupations.foodproducer.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.registry.CraftboundBlockEntities;
import com.magu1436.craftbound.registry.CraftboundItems;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQuality;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityData;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityItems;
import com.magu1436.craftbound.occupations.foodproducer.skills.FoodProducerExperience;
import com.magu1436.craftbound.occupations.foodproducer.skills.FoodProducerPendingExperience;
import com.magu1436.craftbound.occupations.foodproducer.skills.FoodProducerSkills;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

/** 手動加工とCreate自動加工を管理し、開始時の材料・ランク・品質を固定する。 */
public final class FoodProcessingBlockEntity extends BaseContainerBlockEntity {

    public static final int INPUT_0 = 0;
    public static final int INPUT_1 = 1;
    public static final int INPUT_2 = 2;
    public static final int TOOL = 3;
    public static final int OUTPUT = 4;
    public static final int RETURN = 5;
    public static final int FUEL = 6;
    public static final int CONTAINER_SIZE = 7;

    public static final int DATA_STATION = 0;
    public static final int DATA_OPERATION = 1;
    public static final int DATA_PROGRESS = 2;
    public static final int DATA_TOTAL = 3;
    public static final int DATA_RUNNING = 4;
    public static final int DATA_BURN_TIME = 5;
    public static final int DATA_BURN_TOTAL = 6;
    public static final int DATA_COUNT = 7;

    private static final String OPERATION_TAG = "operation";
    private static final String PROGRESS_TAG = "progress";
    private static final String TOTAL_TAG = "total";
    private static final String PENDING_OUTPUT_TAG = "pending_output";
    private static final String PENDING_RETURN_TAG = "pending_return";
    private static final String PENDING_INPUT_TAG = "pending_input_";
    private static final String CONSUMED_TAG = "consumed_";
    private static final String INITIATOR_TAG = "initiator";
    private static final String PENDING_EXPERIENCE_TAG = "pending_experience";
    private static final String BURN_TIME_TAG = "burn_time";
    private static final String BURN_TOTAL_TAG = "burn_total";
    private static final String AUTOMATED_TAG = "automated";

    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private FoodProcessingOperation operation;
    private int progress;
    private int totalTicks;
    private ItemStack pendingOutput = ItemStack.EMPTY;
    private ItemStack pendingReturn = ItemStack.EMPTY;
    private final NonNullList<ItemStack> pendingInputs = NonNullList.withSize(3, ItemStack.EMPTY);
    private final int[] pendingConsumed = new int[3];
    private int pendingExperience;
    private int burnTime;
    private int burnTotal;
    private boolean automated;
    @Nullable
    private UUID initiator;
    private LazyOptional<IItemHandler> createItemHandler = LazyOptional.of(this::createItemHandler);

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_STATION -> station().ordinal();
                case DATA_OPERATION -> currentOperation().ordinal();
                case DATA_PROGRESS -> progress;
                case DATA_TOTAL -> totalTicks;
                case DATA_RUNNING -> isRunning() ? 1 : 0;
                case DATA_BURN_TIME -> burnTime;
                case DATA_BURN_TOTAL -> burnTotal;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_OPERATION -> operation = operationByOrdinal(value);
                case DATA_PROGRESS -> progress = Math.max(0, value);
                case DATA_TOTAL -> totalTicks = Math.max(0, value);
                case DATA_BURN_TIME -> burnTime = Math.max(0, value);
                case DATA_BURN_TOTAL -> burnTotal = Math.max(0, value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public FoodProcessingBlockEntity(BlockPos pos, BlockState state) {
        super(CraftboundBlockEntities.FOOD_PROCESSING.get(), pos, state);
        operation = FoodProcessingStation.fromBlock(state.getBlock()).defaultOperation();
    }

    public FoodProcessingStation station() {
        return FoodProcessingStation.fromBlock(getBlockState().getBlock());
    }

    public FoodProcessingOperation currentOperation() {
        FoodProcessingStation station = station();
        if (station != FoodProcessingStation.COOKING_TABLE) {
            return station.defaultOperation();
        }
        return operation == FoodProcessingOperation.MIX ? FoodProcessingOperation.MIX : FoodProcessingOperation.CUT;
    }

    public boolean selectOperation(FoodProcessingOperation requested) {
        if (isRunning() || station() != FoodProcessingStation.COOKING_TABLE
                || (requested != FoodProcessingOperation.CUT && requested != FoodProcessingOperation.MIX)) {
            return false;
        }
        operation = requested;
        setChanged();
        return true;
    }

    public boolean start(ServerPlayer player) {
        if (level == null || isRunning()
                || !FoodProducerSkills.has(player, FoodProducerSkills.BASIC_PROCESSING)) {
            return false;
        }

        boolean qualityChanged = false;
        for (int slot = INPUT_0; slot <= INPUT_2; slot++) {
            qualityChanged |= FoodQualityData.advanceLoadedTime(
                    items.get(slot),
                    level.getGameTime(),
                    1.0D
            );
        }
        if (qualityChanged) {
            setChanged();
        }
        List<ItemStack> inputs = inputCopies();
        FoodProcessingRecipes.Match match = FoodProcessingRecipes.find(currentOperation(), inputs).orElse(null);
        if (match == null || match.qualityInputs().stream().anyMatch(FoodQualityData::isSpoiled)) {
            return false;
        }
        if (match.requiredRecipeRank() > FoodProducerSkills.recipeResearchRank(player)) {
            return false;
        }
        if (match.toolRequired() && !isUsableKnife(items.get(TOOL))) {
            return false;
        }
        if (currentOperation() == FoodProcessingOperation.HEAT
                && burnTime <= 0
                && fuelBurnTime(items.get(FUEL)) <= 0) {
            return false;
        }

        int rank = Math.max(0, Math.min(3,
                FoodProducerSkills.rank(player, FoodProducerSkills.PROCESSING_TECHNIQUE)));
        ItemStack output = match.output().copy();
        if (output.getItem() instanceof FoodIntermediateItem) {
            FoodIntermediateData.setSuccess(output);
        }
        if (rank >= 2 && appliesExtraOutput(currentOperation())) {
            output.grow(rank - 1);
        }
        if (currentOperation() == FoodProcessingOperation.HEAT) {
            applyFinalCookingQuality(player, match, output);
            pendingExperience = FoodCookingData.experience(output);
        } else {
            FoodQualityData.inheritMinimum(match.qualityInputs(), output, level.getGameTime());
            pendingExperience = output.getItem() instanceof FoodDishItem
                    ? FoodCookingData.experience(output)
                    : 1;
        }

        pendingOutput = output;
        pendingReturn = match.returnedContainer().copy();
        System.arraycopy(match.consumed(), 0, pendingConsumed, 0, pendingConsumed.length);
        capturePendingInputs();
        totalTicks = currentOperation() == FoodProcessingOperation.HEAT
                ? currentOperation().baseTicks()
                : Math.max(1, Math.round(currentOperation().baseTicks() * (1.0F - rank * 0.1F)));
        progress = 0;
        initiator = player.getUUID();
        automated = false;
        setChanged();
        return true;
    }

    /** Createのデプロイヤーから開始する、保存食系列専用の低品質処理。 */
    public boolean startCreateAutomation() {
        if (level == null || isRunning()) return false;

        boolean qualityChanged = false;
        for (int slot = INPUT_0; slot <= INPUT_2; slot++) {
            qualityChanged |= FoodQualityData.advanceLoadedTime(
                    items.get(slot),
                    level.getGameTime(),
                    1.0D
            );
        }
        if (qualityChanged) setChanged();

        FoodProcessingRecipes.AutomatedMatch automatedMatch = FoodProcessingRecipes
                .findCreateAutomation(station(), inputCopies())
                .orElse(null);
        if (automatedMatch == null) return false;

        FoodProcessingRecipes.Match match = automatedMatch.match();
        if (match.qualityInputs().stream().anyMatch(FoodQualityData::isSpoiled)) return false;
        if (match.toolRequired() && !isUsableKnife(items.get(TOOL))) return false;

        ItemStack output = match.output().copy();
        if (output.getItem() instanceof FoodIntermediateItem) {
            FoodIntermediateData.setSuccess(output);
        }
        if (!FoodQualityData.initialize(output, FoodQuality.LOW, level.getGameTime())) {
            return false;
        }

        operation = automatedMatch.operation();
        pendingOutput = output;
        pendingReturn = match.returnedContainer().copy();
        System.arraycopy(match.consumed(), 0, pendingConsumed, 0, pendingConsumed.length);
        capturePendingInputs();
        totalTicks = currentOperation().baseTicks();
        progress = 0;
        initiator = null;
        pendingExperience = 0;
        automated = true;
        setChanged();
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FoodProcessingBlockEntity processor) {
        if (level.isClientSide) {
            return;
        }
        boolean changed = false;
        if (processor.station() == FoodProcessingStation.COOKING_POT && processor.burnTime > 0) {
            processor.burnTime--;
            changed = true;
        }
        if (level.getGameTime() % 20L == 0L) {
            boolean qualityChanged = false;
            for (int slot = INPUT_0; slot <= INPUT_2; slot++) {
                ItemStack input = processor.items.get(slot);
                if (!input.isEmpty() && (!processor.isRunning() || processor.pendingInputs.get(slot).isEmpty())) {
                    qualityChanged |= FoodQualityData.advanceLoadedTime(
                            input,
                            level.getGameTime(),
                            1.0D
                    );
                }
            }
            if (qualityChanged) {
                changed = true;
            }
        }
        if (!processor.isRunning()) {
            if (changed) processor.setChanged();
            return;
        }

        if (!processor.hasRequiredInputs()) {
            processor.decayOrCancel();
            if (changed) processor.setChanged();
            return;
        }

        if (processor.progress >= processor.totalTicks) {
            if (processor.canFinish()) processor.finish();
            else if (changed) processor.setChanged();
            return;
        }

        if (processor.currentOperation() == FoodProcessingOperation.HEAT) {
            if (processor.burnTime <= 0 && !processor.consumeFuel()) {
                processor.decayOrCancel();
                if (changed) processor.setChanged();
                return;
            }
        }
        processor.progress++;
        if (processor.progress >= processor.totalTicks && processor.canFinish()) processor.finish();
        processor.setChanged();
    }

    private boolean canFinish() {
        return canMerge(items.get(OUTPUT), pendingOutput) && canMerge(items.get(RETURN), pendingReturn);
    }

    private void finish() {
        for (int slot = 0; slot < pendingConsumed.length; slot++) {
            items.get(slot).shrink(pendingConsumed[slot]);
        }
        if (currentOperation() == FoodProcessingOperation.CUT) {
            ItemStack knife = items.get(TOOL);
            if (!knife.isEmpty()) {
                knife.setDamageValue(knife.getDamageValue() + 1);
                if (knife.getDamageValue() >= knife.getMaxDamage()) {
                    items.set(TOOL, ItemStack.EMPTY);
                }
            }
        }
        mergeInto(OUTPUT, pendingOutput);
        mergeInto(RETURN, pendingReturn);
        if (level instanceof ServerLevel serverLevel && initiator != null) {
            ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(initiator);
            boolean delivered = player != null && FoodProducerExperience.add(player, pendingExperience);
            if (!delivered) {
                FoodProducerPendingExperience.queue(serverLevel.getServer(), initiator, pendingExperience);
            }
        }
        clearPending();
    }

    public boolean finishForTesting() {
        if (!isRunning()) {
            return false;
        }
        progress = Math.max(progress, totalTicks - 20);
        setChanged();
        return true;
    }

    public boolean isRunning() {
        return !pendingOutput.isEmpty() && totalTicks > 0;
    }

    public int progress() {
        return progress;
    }

    public int totalTicks() {
        return totalTicks;
    }

    public int burnTime() {
        return burnTime;
    }

    public int burnTotal() {
        return burnTotal;
    }

    public boolean isAutomated() {
        return automated;
    }

    public ContainerData dataAccess() {
        return dataAccess;
    }

    public boolean slotLocked(int slot) {
        return isRunning() && slot >= INPUT_0 && slot <= TOOL;
    }

    public boolean playerCanModifySlot(int slot) {
        return !isRunning() || slot >= INPUT_0 && slot <= INPUT_2 || slot > TOOL;
    }

    ItemStack removeInputForPlayer(int slot, int amount) {
        if (slot < INPUT_0 || slot > INPUT_2) return ItemStack.EMPTY;
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }

    void setInputForPlayer(int slot, ItemStack stack) {
        if (slot < INPUT_0 || slot > INPUT_2) return;
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.craftbound.food_processing");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new FoodProcessingMenu(containerId, inventory, this, dataAccess);
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
        if (slotLocked(slot)) return ItemStack.EMPTY;
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slotLocked(slot)) return ItemStack.EMPTY;
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slotLocked(slot)) return;
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slotLocked(slot) || slot == OUTPUT || slot == RETURN) return false;
        if (slot == FUEL) return station() == FoodProcessingStation.COOKING_POT && fuelBurnTime(stack) > 0;
        return slot != TOOL || stack.is(CraftboundItems.COOKING_KNIFE.get());
    }

    @Override
    public void clearContent() {
        if (!isRunning()) {
            items.clear();
            setChanged();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D,
                        worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putString(OPERATION_TAG, currentOperation().serializedName());
        tag.putInt(PROGRESS_TAG, progress);
        tag.putInt(TOTAL_TAG, totalTicks);
        if (!pendingOutput.isEmpty()) tag.put(PENDING_OUTPUT_TAG, pendingOutput.save(new CompoundTag()));
        if (!pendingReturn.isEmpty()) tag.put(PENDING_RETURN_TAG, pendingReturn.save(new CompoundTag()));
        for (int slot = 0; slot < pendingConsumed.length; slot++) {
            if (!pendingInputs.get(slot).isEmpty()) {
                tag.put(PENDING_INPUT_TAG + slot, pendingInputs.get(slot).save(new CompoundTag()));
            }
            tag.putInt(CONSUMED_TAG + slot, pendingConsumed[slot]);
        }
        if (initiator != null) tag.putUUID(INITIATOR_TAG, initiator);
        tag.putInt(PENDING_EXPERIENCE_TAG, pendingExperience);
        tag.putInt(BURN_TIME_TAG, burnTime);
        tag.putInt(BURN_TOTAL_TAG, burnTotal);
        tag.putBoolean(AUTOMATED_TAG, automated);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        operation = operationByName(tag.getString(OPERATION_TAG));
        progress = Math.max(0, tag.getInt(PROGRESS_TAG));
        totalTicks = Math.max(0, tag.getInt(TOTAL_TAG));
        pendingOutput = tag.contains(PENDING_OUTPUT_TAG) ? ItemStack.of(tag.getCompound(PENDING_OUTPUT_TAG)) : ItemStack.EMPTY;
        pendingReturn = tag.contains(PENDING_RETURN_TAG) ? ItemStack.of(tag.getCompound(PENDING_RETURN_TAG)) : ItemStack.EMPTY;
        for (int slot = 0; slot < pendingConsumed.length; slot++) {
            pendingInputs.set(slot, tag.contains(PENDING_INPUT_TAG + slot)
                    ? ItemStack.of(tag.getCompound(PENDING_INPUT_TAG + slot))
                    : ItemStack.EMPTY);
            pendingConsumed[slot] = tag.getInt(CONSUMED_TAG + slot);
        }
        initiator = tag.hasUUID(INITIATOR_TAG) ? tag.getUUID(INITIATOR_TAG) : null;
        pendingExperience = Math.max(0, tag.getInt(PENDING_EXPERIENCE_TAG));
        burnTime = Math.max(0, tag.getInt(BURN_TIME_TAG));
        burnTotal = Math.max(0, tag.getInt(BURN_TOTAL_TAG));
        automated = tag.getBoolean(AUTOMATED_TAG);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER && ModList.get().isLoaded("create")) {
            return createItemHandler.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        createItemHandler.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        createItemHandler = LazyOptional.of(this::createItemHandler);
    }

    private List<ItemStack> inputCopies() {
        List<ItemStack> inputs = new ArrayList<>(3);
        for (int slot = INPUT_0; slot <= INPUT_2; slot++) inputs.add(items.get(slot).copy());
        return inputs;
    }

    private void clearPending() {
        pendingOutput = ItemStack.EMPTY;
        pendingReturn = ItemStack.EMPTY;
        for (int slot = 0; slot < pendingInputs.size(); slot++) pendingInputs.set(slot, ItemStack.EMPTY);
        java.util.Arrays.fill(pendingConsumed, 0);
        progress = 0;
        totalTicks = 0;
        initiator = null;
        pendingExperience = 0;
        automated = false;
        setChanged();
    }

    private void capturePendingInputs() {
        for (int slot = 0; slot < pendingConsumed.length; slot++) {
            if (pendingConsumed[slot] <= 0) {
                pendingInputs.set(slot, ItemStack.EMPTY);
                continue;
            }
            ItemStack required = items.get(slot).copy();
            required.setCount(pendingConsumed[slot]);
            pendingInputs.set(slot, required);
        }
    }

    private boolean hasRequiredInputs() {
        for (int slot = 0; slot < pendingInputs.size(); slot++) {
            ItemStack required = pendingInputs.get(slot);
            if (required.isEmpty()) {
                if (pendingConsumed[slot] > 0) return false;
                continue;
            }
            ItemStack current = items.get(slot);
            if (current.getCount() < required.getCount()
                    || !ItemStack.isSameItemSameTags(current, required)) {
                return false;
            }
        }
        return true;
    }

    private void decayOrCancel() {
        if (progress > 0) {
            progress = Math.max(0, progress - 2);
        }
        if (progress == 0) {
            clearPending();
        } else {
            setChanged();
        }
    }

    private void applyFinalCookingQuality(
            ServerPlayer player,
            FoodProcessingRecipes.Match match,
            ItemStack output
    ) {
        FoodQuality baseQuality = match.qualityInputs().stream()
                .filter(FoodQualityItems::isQualityTarget)
                .map(FoodQualityData::getOrStandard)
                .min(java.util.Comparator.comparingInt(FoodQuality::value))
                .orElse(FoodQuality.STANDARD);
        FoodQuality cap = match.qualityInputs().stream()
                .filter(FoodCookingData::isPreparedSet)
                .map(FoodCookingData::qualityCap)
                .findFirst()
                .orElse(baseQuality);
        int qualityRank = Math.max(0, Math.min(5,
                FoodProducerSkills.rank(player, FoodProducerSkills.QUALITY_COOKING)));
        boolean eligible = qualityRank > 0 && baseQuality.value() < cap.value();
        boolean forced = eligible && FoodCookingTestHooks.consumeForcedUpgrade(player);
        int ratingModifier = switch (FoodCookingData.rating(output)) {
            case 0 -> -20;
            case 2 -> 20;
            case 3 -> 40;
            default -> 0;
        };
        int upgradeChance = Math.max(0, Math.min(100, qualityRank * 10 + ratingModifier));
        boolean upgraded = eligible && (forced || level.random.nextInt(100) < upgradeChance);
        FoodQuality finalQuality = upgraded
                ? FoodQuality.fromValue(baseQuality.value() + 1)
                : baseQuality;
        FoodQualityData.initialize(output, finalQuality, level.getGameTime());
    }

    private boolean consumeFuel() {
        ItemStack fuel = items.get(FUEL);
        int duration = fuelBurnTime(fuel);
        if (duration <= 0) return false;
        ItemStack remainder = fuel.getCraftingRemainingItem();
        if (fuel.getCount() > 1 && !canMerge(items.get(RETURN), remainder)) return false;

        burnTime = duration;
        burnTotal = duration;
        fuel.shrink(1);
        if (fuel.isEmpty()) {
            items.set(FUEL, remainder);
        } else {
            mergeInto(RETURN, remainder);
        }
        setChanged();
        return true;
    }

    private void mergeInto(int slot, ItemStack added) {
        if (added.isEmpty()) return;
        ItemStack stored = items.get(slot);
        if (stored.isEmpty()) {
            items.set(slot, added.copy());
        } else {
            stored.grow(added.getCount());
        }
    }

    private boolean canMerge(ItemStack stored, ItemStack added) {
        if (added.isEmpty()) return true;
        if (stored.isEmpty()) return added.getCount() <= added.getMaxStackSize();
        if (stored.getCount() + added.getCount() > stored.getMaxStackSize()) return false;
        if (ItemStack.isSameItemSameTags(stored, added)) return true;
        return level != null
                && FoodQualityData.prepareForMerge(stored, added, level.getGameTime(), 1.0D);
    }

    private IItemHandler createItemHandler() {
        return new CreateItemHandler(this);
    }

    /** Createの搬送にだけ公開する。バニラのホッパーはMixin側で遮断する。 */
    private final class CreateItemHandler extends InvWrapper {

        private CreateItemHandler(FoodProcessingBlockEntity processor) {
            super(processor);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (isRunning()) return false;
            if (slot == TOOL) return stack.is(CraftboundItems.COOKING_KNIFE.get());
            return slot >= INPUT_0 && slot <= INPUT_2
                    && !stack.is(CraftboundItems.COOKING_KNIFE.get());
        }

        @Override
        @NotNull
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !isItemValid(slot, stack)) return stack;

            ItemStack stored = items.get(slot);
            int limit = Math.min(getSlotLimit(slot), stack.getMaxStackSize());
            int space = stored.isEmpty() ? limit : limit - stored.getCount();
            if (space <= 0) return stack;

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

            if (inserted >= stack.getCount()) return ItemStack.EMPTY;
            ItemStack remainder = stack.copy();
            remainder.shrink(inserted);
            return remainder;
        }

        @Override
        @NotNull
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != OUTPUT && slot != RETURN) return ItemStack.EMPTY;
            return super.extractItem(slot, amount, simulate);
        }
    }

    private static boolean isUsableKnife(ItemStack stack) {
        return stack.is(CraftboundItems.COOKING_KNIFE.get())
                && stack.getDamageValue() < stack.getMaxDamage();
    }

    private static boolean appliesExtraOutput(FoodProcessingOperation operation) {
        return operation == FoodProcessingOperation.CUT
                || operation == FoodProcessingOperation.GRIND
                || operation == FoodProcessingOperation.PRESERVE;
    }

    private static int fuelBurnTime(ItemStack stack) {
        return stack.isEmpty() ? 0 : ForgeHooks.getBurnTime(stack, RecipeType.SMELTING);
    }

    private static FoodProcessingOperation operationByName(String name) {
        for (FoodProcessingOperation value : FoodProcessingOperation.values()) {
            if (value.serializedName().equals(name)) return value;
        }
        return FoodProcessingOperation.CUT;
    }

    private static FoodProcessingOperation operationByOrdinal(int ordinal) {
        FoodProcessingOperation[] values = FoodProcessingOperation.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : FoodProcessingOperation.CUT;
    }
}
