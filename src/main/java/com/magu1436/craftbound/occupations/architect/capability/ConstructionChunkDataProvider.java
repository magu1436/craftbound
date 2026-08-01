package com.magu1436.craftbound.occupations.architect.capability;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import com.magu1436.craftbound.registry.CraftboundCapabilities;

/**
 * 建築チャンクデータCapabilityをLevelChunkへ公開するProvider。
 */
public final class ConstructionChunkDataProvider
    implements ICapabilitySerializable<CompoundTag> {

    private final ConstructionChunkData data;
    private final LazyOptional<ConstructionChunkData> optional;

    public ConstructionChunkDataProvider(int minBuildHeight) {
        data = new ConstructionChunkDataImpl(minBuildHeight);
        optional = LazyOptional.of(() -> data);
    }

    @Override
    public <T> LazyOptional<T> getCapability(
        Capability<T> capability,
        @Nullable Direction side
    ) {
        return CraftboundCapabilities.CONSTRUCTION_CHUNK_DATA.orEmpty(
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
