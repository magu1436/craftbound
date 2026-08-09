package com.magu1436.craftbound.occupations.blacksmith.furnace;

import com.magu1436.craftbound.registry.CraftboundBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class BlacksmithFurnaceBlockEntity extends BlockEntity {

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
    }

    @Override
    public void onLoad() {
        super.onLoad();
        refreshHeatSource();
    }

    public void refreshHeatSource() {
        // Implemented in step 3; this method is the cache-update boundary.
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
