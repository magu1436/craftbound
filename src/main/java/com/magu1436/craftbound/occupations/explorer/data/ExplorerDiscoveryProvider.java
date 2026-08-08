package com.magu1436.craftbound.occupations.explorer.data;

import javax.annotation.Nullable;

import com.magu1436.craftbound.registry.CraftboundCapabilities;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

/** 発見履歴Capabilityをプレイヤーへ公開するProvider。 */
public final class ExplorerDiscoveryProvider
    implements ICapabilitySerializable<CompoundTag> {

    private final ExplorerDiscoveryDataImpl data =
        new ExplorerDiscoveryDataImpl();
    private final LazyOptional<ExplorerDiscoveryData> optional =
        LazyOptional.of(() -> data);

    @Override
    public <T> LazyOptional<T> getCapability(
        Capability<T> capability,
        @Nullable Direction side
    ) {
        return CraftboundCapabilities.EXPLORER_DISCOVERY_DATA.orEmpty(
            capability,
            optional
        );
    }

    @Override
    public CompoundTag serializeNBT() {
        return ExplorerDiscoverySerializer.serialize(data);
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        ExplorerDiscoverySerializer.deserialize(tag, data);
    }

    public void invalidate() {
        optional.invalidate();
    }
}
