package com.magu1436.craftbound.occupations.blacksmith.casting;

import java.util.Optional;
import java.util.OptionalInt;

import javax.annotation.Nullable;

import com.magu1436.craftbound.occupations.blacksmith.casting.lump.MetalLumpStateService;
import com.magu1436.craftbound.occupations.blacksmith.casting.part.RoughMetalPartStateService;
import com.magu1436.craftbound.occupations.blacksmith.casting.definition.MetalPartDefinitions;
import com.magu1436.craftbound.occupations.blacksmith.crucible.CrucibleItem;
import com.magu1436.craftbound.registry.CraftboundBlockEntities;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

public final class CastingTableBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CURRENT_VERSION = 1;
    private static final String TAG_VERSION = "Version";
    private static final String TAG_MOLD = "Mold";
    private static final String TAG_ACTIVE_PROCESS = "ActiveProcess";
    private static final String TAG_PENDING_OUTPUT = "PendingOutput";

    private ItemStack mold = ItemStack.EMPTY;
    @Nullable
    private CastingProcess activeProcess;
    private ItemStack pendingOutput = ItemStack.EMPTY;
    private boolean invalidStoredState;
    private boolean transactionInProgress;
    @Nullable
    private CompoundTag invalidStoredData;

    public CastingTableBlockEntity(
        BlockPos pos,
        BlockState state
    ) {
        this(CraftboundBlockEntities.CASTING_TABLE.get(), pos, state);
    }

    public CastingTableBlockEntity(
        BlockEntityType<?> type,
        BlockPos pos,
        BlockState state
    ) {
        super(type, pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (invalidStoredState && invalidStoredData != null) {
            preserveStoredTag(tag, invalidStoredData, TAG_VERSION);
            preserveStoredTag(tag, invalidStoredData, TAG_MOLD);
            preserveStoredTag(tag, invalidStoredData, TAG_ACTIVE_PROCESS);
            preserveStoredTag(tag, invalidStoredData, TAG_PENDING_OUTPUT);
            return;
        }
        tag.putInt(TAG_VERSION, CURRENT_VERSION);
        if (!mold.isEmpty()) {
            tag.put(TAG_MOLD, mold.save(new CompoundTag()));
        }
        if (activeProcess != null) {
            tag.put(TAG_ACTIVE_PROCESS, CastingProcessCodec.write(activeProcess));
        }
        if (!pendingOutput.isEmpty()) {
            tag.put(TAG_PENDING_OUTPUT, pendingOutput.save(new CompoundTag()));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        boolean valid = tag.contains(TAG_VERSION, Tag.TAG_INT)
            && tag.getInt(TAG_VERSION) == CURRENT_VERSION;

        ItemStack loadedMold = ItemStack.EMPTY;
        CastingProcess loadedProcess = null;
        ItemStack loadedPending = ItemStack.EMPTY;
        if (valid && tag.contains(TAG_MOLD)) {
            valid = tag.contains(TAG_MOLD, Tag.TAG_COMPOUND);
            if (valid) {
                loadedMold = ItemStack.of(tag.getCompound(TAG_MOLD));
                valid = loadedMold.getCount() == 1
                    && MetalPartDefinitions.INSTANCE.isRegisteredMold(loadedMold);
            }
        }
        if (valid && tag.contains(TAG_ACTIVE_PROCESS)) {
            valid = tag.contains(TAG_ACTIVE_PROCESS, Tag.TAG_COMPOUND);
            if (valid) {
                Optional<CastingProcess> result = CastingProcessCodec.read(
                    tag.getCompound(TAG_ACTIVE_PROCESS)
                );
                valid = result.isPresent();
                loadedProcess = result.orElse(null);
            }
        }
        if (valid && tag.contains(TAG_PENDING_OUTPUT)) {
            valid = tag.contains(TAG_PENDING_OUTPUT, Tag.TAG_COMPOUND);
            if (valid) {
                loadedPending = ItemStack.of(tag.getCompound(TAG_PENDING_OUTPUT));
                valid = isValidPendingOutput(loadedPending);
            }
        }
        if (valid) {
            valid = isConsistent(loadedMold, loadedProcess, loadedPending);
        }

        mold = loadedMold;
        activeProcess = loadedProcess;
        pendingOutput = loadedPending;
        invalidStoredState = !valid;
        transactionInProgress = false;
        invalidStoredData = valid ? null : tag.copy();
        if (!valid) {
            LOGGER.error(
                "Invalid casting table state at {}: unsupported version, malformed data, or inconsistent contents",
                worldPosition
            );
        }
    }

    public boolean tryInsertMold(ServerPlayer player) {
        if (player == null || invalidStoredState || transactionInProgress
            || !mold.isEmpty() || activeProcess != null || !pendingOutput.isEmpty()) {
            return false;
        }
        ItemStack held = player.getMainHandItem();
        if (!MetalPartDefinitions.INSTANCE.isRegisteredMold(held)) {
            return false;
        }
        mold = held.split(1);
        markStateChangedAndSync();
        return true;
    }

    public boolean tryRemoveMold(ServerPlayer player) {
        if (player == null || invalidStoredState || transactionInProgress
            || mold.isEmpty() || activeProcess != null || !pendingOutput.isEmpty()
            || !player.getMainHandItem().isEmpty() || !player.isShiftKeyDown()) {
            return false;
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, mold);
        mold = ItemStack.EMPTY;
        markStateChangedAndSync();
        return true;
    }

    public boolean tryCollectPendingOutput(ServerPlayer player) {
        if (player == null || invalidStoredState || pendingOutput.isEmpty()
            || !player.getMainHandItem().isEmpty()) {
            return false;
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, pendingOutput);
        pendingOutput = ItemStack.EMPTY;
        markStateChangedAndSync();
        return true;
    }

    public boolean tryFinalizeAndCollect(ServerPlayer player) {
        if (player == null || invalidStoredState || transactionInProgress
            || activeProcess == null || !pendingOutput.isEmpty()
            || !player.getMainHandItem().isEmpty()) {
            return false;
        }
        if (!finalizeActiveProcess()) {
            return false;
        }
        return tryCollectPendingOutput(player);
    }

    public CastingActionResult tryPour(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof CrucibleItem)) {
            return CastingActionResult.PASS;
        }
        return CastingGameService.tryPour(player, this, held);
    }

    public ItemStack getMoldForRendering() {
        return mold.copy();
    }

    @Nullable
    public CastingProcess getActiveProcessForRendering() {
        return activeProcess;
    }

    public void dropStoredContents(ServerLevel level) {
        ItemStack moldToDrop = mold;
        ItemStack outputToDrop = pendingOutput;
        if (outputToDrop.isEmpty() && activeProcess != null) {
            if (finalizeActiveProcess()) {
                outputToDrop = pendingOutput;
            } else {
                LOGGER.error("Could not finalize casting process while breaking table at {}", worldPosition);
            }
        }

        mold = ItemStack.EMPTY;
        activeProcess = null;
        pendingOutput = ItemStack.EMPTY;
        invalidStoredState = false;
        transactionInProgress = false;
        invalidStoredData = null;
        setChanged();
        drop(level, worldPosition, moldToDrop);
        drop(level, worldPosition, outputToDrop);
    }

    public static void serverTick(
        Level level,
        BlockPos pos,
        BlockState state,
        CastingTableBlockEntity table
    ) {
        if (!(level instanceof ServerLevel serverLevel)
            || table.invalidStoredState
            || table.activeProcess == null) {
            return;
        }

        CastingProcess previous = table.activeProcess;
        long coolingTicks = previous.coolingTicks() == Long.MAX_VALUE
            ? Long.MAX_VALUE : previous.coolingTicks() + 1L;
        long surfaceSolidTicks = previous.definitionSnapshot().cooling().surfaceSolidTicks();
        boolean reachedSurface = coolingTicks >= surfaceSolidTicks;
        boolean notify = !previous.surfaceSolidificationNotified() && reachedSurface;
        table.activeProcess = copyWithCooling(previous, coolingTicks,
            previous.surfaceSolidificationNotified() || reachedSurface);
        table.setChanged();

        boolean thresholdCrossed = previous.coolingTicks() < surfaceSolidTicks
            && coolingTicks >= surfaceSolidTicks;
        if (notify && thresholdCrossed) {
            table.playSurfaceSolidificationEffect(serverLevel);
        }
        if (notify || coolingTicks % 5L == 0L) {
            table.syncToClient();
        }
    }

    boolean canStartCasting() {
        return !invalidStoredState && !transactionInProgress
            && activeProcess == null && pendingOutput.isEmpty() && !mold.isEmpty();
    }

    ItemStack mold() {
        return mold;
    }

    void beginTransaction() {
        transactionInProgress = true;
    }

    void endTransaction() {
        transactionInProgress = false;
    }

    void setActiveProcess(CastingProcess process) {
        activeProcess = process;
        markStateChangedAndSync();
    }

    void setPendingOutput(ItemStack output) {
        pendingOutput = output;
        markStateChangedAndSync();
    }

    void markStateChangedAndSync() {
        setChanged();
        syncToClient();
    }

    private boolean finalizeActiveProcess() {
        CastingProcess process = activeProcess;
        if (process == null) {
            return false;
        }
        OptionalInt breakLimit = CoolingBreakLimitService.evaluate(
            process.definitionSnapshot(),
            process.coolingTicks()
        );
        if (breakLimit.isEmpty()) {
            return false;
        }
        Optional<ItemStack> output = RoughMetalPartStateService.create(
            process.processId(),
            process.operatorId(),
            process.definitionSnapshot(),
            process.heatingTicks(),
            process.heatingScore(),
            process.coolingTicks(),
            breakLimit.getAsInt(),
            process.visualData()
        );
        if (output.isEmpty()
            || !process.definitionSnapshot().roughOutputItemId().equals(
                ForgeRegistries.ITEMS.getKey(output.get().getItem()))) {
            return false;
        }
        pendingOutput = output.get();
        activeProcess = null;
        markStateChangedAndSync();
        return true;
    }

    private void playSurfaceSolidificationEffect(ServerLevel level) {
        level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH,
            SoundSource.BLOCKS, 0.5F, 1.1F);
        level.sendParticles(ParticleTypes.CLOUD,
            worldPosition.getX() + 0.5D,
            worldPosition.getY() + 0.9D,
            worldPosition.getZ() + 0.5D,
            4, 0.2D, 0.05D, 0.2D, 0.01D);
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private static CastingProcess copyWithCooling(
        CastingProcess process,
        long coolingTicks,
        boolean notified
    ) {
        return new CastingProcess(process.version(), process.processId(), process.operatorId(),
            process.partDefinitionId(), process.metalId(), process.metalAmount(),
            process.heatingTicks(), process.heatingScore(), coolingTicks, notified,
            process.definitionSnapshot(), process.visualData());
    }

    private static boolean isConsistent(
        ItemStack mold,
        @Nullable CastingProcess process,
        ItemStack pending
    ) {
        if (process != null && (mold.isEmpty() || !pending.isEmpty())) {
            return false;
        }
        if (process == null) {
            return true;
        }
        return process.definitionSnapshot().moldItemId().equals(
            ForgeRegistries.ITEMS.getKey(mold.getItem())
        );
    }

    private static boolean isValidPendingOutput(ItemStack output) {
        return !output.isEmpty()
            && (RoughMetalPartStateService.read(output).isPresent()
                || MetalLumpStateService.read(output).isPresent());
    }

    private static void drop(ServerLevel level, BlockPos pos, ItemStack stack) {
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5D,
                pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);
        }
    }

    private static void preserveStoredTag(
        CompoundTag target,
        CompoundTag source,
        String key
    ) {
        Tag value = source.get(key);
        if (value != null) {
            target.put(key, value.copy());
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        if (tag != null) {
            load(tag);
        }
    }
}
