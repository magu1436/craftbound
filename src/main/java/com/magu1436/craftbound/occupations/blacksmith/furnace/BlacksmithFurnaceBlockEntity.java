package com.magu1436.craftbound.occupations.blacksmith.furnace;

import com.magu1436.craftbound.occupations.blacksmith.crucible.CrucibleItem;
import com.magu1436.craftbound.occupations.blacksmith.crucible.CrucibleStateService;
import com.magu1436.craftbound.registry.CraftboundBlockEntities;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
        // Implemented in step 3; this method is the cache-update boundary.
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
        if (hand != InteractionHand.MAIN_HAND || hasCrucible()) {
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
        if (!CrucibleStateService.advanceHeating(
            crucible,
            pendingHeatingTicks
        )) {
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
        // Heating is implemented in step 3.
    }
}
