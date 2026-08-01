package com.magu1436.craftbound.occupations.architect.capability;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import com.magu1436.craftbound.registry.CraftboundCapabilities;

/**
 * 設置履歴CapabilityをLevelChunkへ公開するProvider。
 */
public final class PlayerPlacedBlockProvider
    implements ICapabilitySerializable<CompoundTag> {

    private final PlayerPlacedBlockData data =
        new PlayerPlacedBlockDataImpl();
    private final LazyOptional<PlayerPlacedBlockData> optional =
        LazyOptional.of(() -> data);

    @Override
    public <T> LazyOptional<T> getCapability(
        Capability<T> capability,
        @Nullable Direction side
    ) {
        return CraftboundCapabilities.PLAYER_PLACED_BLOCKS.orEmpty(
            capability,
            optional
        );
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.savePersistentData();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        data.loadPersistentData(tag);
    }

    public void invalidate() {
        optional.invalidate();
    }
}
