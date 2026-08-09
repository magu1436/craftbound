package com.magu1436.craftbound.occupations.blacksmith.furnace;

import java.util.Optional;

import com.magu1436.craftbound.occupations.blacksmith.crucible.CrucibleItem;
import com.magu1436.craftbound.occupations.blacksmith.crucible.CrucibleProcessState;
import com.magu1436.craftbound.occupations.blacksmith.crucible.CrucibleState;
import com.magu1436.craftbound.occupations.blacksmith.crucible.CrucibleStateService;
import com.magu1436.craftbound.occupations.blacksmith.melting.MeltingGameService;
import com.magu1436.craftbound.occupations.blacksmith.melting.state.HeatingStatus;
import com.magu1436.craftbound.registry.CraftboundBlockEntities;
import com.magu1436.craftbound.registry.CraftboundBlockTags;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

public final class BlacksmithFurnaceBlockEntity extends BlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG_CRUCIBLE = "Crucible";
    private static final String TAG_PENDING_HEATING_TICKS = "PendingHeatingTicks";
    private static final String TAG_WARNING_SOUND_COOLDOWN_TICKS =
            "WarningSoundCooldownTicks";

    private ItemStack crucible = ItemStack.EMPTY;
    private long pendingHeatingTicks;
    private int warningSoundCooldownTicks;
    private boolean hasHeatSource;
    private boolean invalidStoredState;

    public BlacksmithFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(CraftboundBlockEntities.BLACKSMITH_FURNACE.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!crucible.isEmpty()) {
            tag.put(TAG_CRUCIBLE, crucible.save(new CompoundTag()));
        }
        tag.putLong(TAG_PENDING_HEATING_TICKS, pendingHeatingTicks);
        tag.putInt(TAG_WARNING_SOUND_COOLDOWN_TICKS, warningSoundCooldownTicks);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        crucible = tag.contains(TAG_CRUCIBLE)
            ? ItemStack.of(tag.getCompound(TAG_CRUCIBLE))
            : ItemStack.EMPTY;
        pendingHeatingTicks = tag.getLong(TAG_PENDING_HEATING_TICKS);
        warningSoundCooldownTicks = tag.getInt(TAG_WARNING_SOUND_COOLDOWN_TICKS);
        invalidStoredState = pendingHeatingTicks < 0L
            || (!crucible.isEmpty()
                && CrucibleStateService.read(crucible).isEmpty());
    }

    @Override
    public void onLoad() {
        super.onLoad();
        refreshHeatSource();
    }

    public void refreshHeatSource() {
        if (level != null) {
            hasHeatSource = level.getBlockState(worldPosition.below()).is(
                CraftboundBlockTags.BLACKSMITH_HEAT_SOURCES
            );
        }
    }

    public boolean hasHeatSource() {
        return hasHeatSource;
    }

    public boolean hasCrucible() {
        return !crucible.isEmpty();
    }

    public ItemStack getCrucible() {
        return crucible.copy();
    }

    public boolean tryInsertCrucible(
        ServerPlayer player,
        InteractionHand hand
    ) {
        if (hand != InteractionHand.MAIN_HAND
            || hasCrucible()
            || invalidStoredState) {
            return false;
        }

        ItemStack heldItem = player.getItemInHand(hand);
        if (!(heldItem.getItem() instanceof CrucibleItem)
            || heldItem.getCount() != 1
            || CrucibleStateService.read(heldItem).isEmpty()) {
            return false;
        }

        crucible = heldItem;
        player.setItemInHand(hand, ItemStack.EMPTY);
        pendingHeatingTicks = 0L;
        warningSoundCooldownTicks = 0;
        invalidStoredState = false;
        setChanged();
        return true;
    }

    public boolean tryExtractCrucible(ServerPlayer player) {
        if (!player.getMainHandItem().isEmpty()
            || !hasCrucible()
            || invalidStoredState
            || !commitPendingHeating()) {
            return false;
        }

        player.setItemInHand(InteractionHand.MAIN_HAND, crucible);
        clearStoredCrucible();
        setChanged();
        return true;
    }

    public void dropStoredCrucible(ServerLevel level) {
        if (!hasCrucible()) {
            return;
        }

        if (!commitPendingHeating()) {
            LOGGER.error(
                "Could not commit {} pending heating ticks for crucible at {} before dropping it",
                pendingHeatingTicks,
                worldPosition
            );
        }
        Containers.dropItemStack(
            level,
            worldPosition.getX() + 0.5D,
            worldPosition.getY() + 0.5D,
            worldPosition.getZ() + 0.5D,
            crucible
        );
        clearStoredCrucible();
        setChanged();
    }

    private boolean commitPendingHeating() {
        if (pendingHeatingTicks < 0L) {
            return false;
        }
        if (pendingHeatingTicks == 0L) {
            return true;
        }
        if (!MeltingGameService.commitHeating(crucible, pendingHeatingTicks)) {
            return false;
        }
        pendingHeatingTicks = 0L;
        return true;
    }

    private void clearStoredCrucible() {
        crucible = ItemStack.EMPTY;
        pendingHeatingTicks = 0L;
        warningSoundCooldownTicks = 0;
        invalidStoredState = false;
    }

    public static void serverTick(
        Level level,
        BlockPos pos,
        BlockState state,
        BlacksmithFurnaceBlockEntity furnace
    ) {
        if (!(level instanceof ServerLevel serverLevel)
            || !furnace.hasCrucible()
            || furnace.invalidStoredState
            || !furnace.hasHeatSource) {
            return;
        }

        Optional<CrucibleState> stateResult =
            CrucibleStateService.read(furnace.crucible);
        if (stateResult.isEmpty()) {
            furnace.invalidStoredState = true;
            LOGGER.error(
                "Stopping blacksmith furnace at {} because its stored crucible state is invalid",
                pos
            );
            return;
        }
        if (stateResult.get().processState() == CrucibleProcessState.EMPTY) {
            return;
        }

        long candidatePendingHeatingTicks = saturatedIncrement(
            furnace.pendingHeatingTicks
        );
        Optional<HeatingStatus> statusResult = MeltingGameService.evaluate(
            furnace.crucible,
            candidatePendingHeatingTicks
        );
        if (statusResult.isEmpty()) {
            return;
        }

        furnace.pendingHeatingTicks = candidatePendingHeatingTicks;
        furnace.processWarning(serverLevel, statusResult.get());
        furnace.setChanged();
    }

    private void processWarning(ServerLevel level, HeatingStatus status) {
        if (!status.warningRequired()) {
            warningSoundCooldownTicks = 0;
            return;
        }

        if (pendingHeatingTicks % 10L == 0L) {
            level.sendParticles(
                ParticleTypes.LARGE_SMOKE,
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 1.05D,
                worldPosition.getZ() + 0.5D,
                1,
                0.15D,
                0.05D,
                0.15D,
                0.01D
            );
        }

        if (warningSoundCooldownTicks <= 0) {
            level.playSound(
                null,
                worldPosition,
                SoundEvents.ANVIL_HIT,
                SoundSource.BLOCKS,
                0.8F,
                0.9F + level.random.nextFloat() * 0.2F
            );
            warningSoundCooldownTicks = 40 + level.random.nextInt(61);
        } else {
            warningSoundCooldownTicks--;
        }
    }

    private static long saturatedIncrement(long value) {
        return value == Long.MAX_VALUE ? Long.MAX_VALUE : value + 1L;
    }
}
