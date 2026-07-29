package com.magu1436.craftbound.common.capability;

import java.util.Objects;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

/**
 * プレイヤーCapabilityをForgeへ公開する共通Provider。
 *
 * @param <T> Capabilityが公開するデータ型
 */
public final class PlayerCapabilityProvider<
    T extends PlayerCapabilityData<T>
> implements ICapabilitySerializable<CompoundTag> {

    private final Capability<T> capability;
    private final T data;
    private final LazyOptional<T> optional;

    public PlayerCapabilityProvider(
        Capability<T> capability,
        Supplier<T> dataFactory
    ) {
        this.capability = Objects.requireNonNull(
            capability,
            "capability is null"
        );
        this.data = Objects.requireNonNull(
            Objects.requireNonNull(
                dataFactory,
                "data factory is null"
            ).get(),
            "capability data is null"
        );
        this.optional = LazyOptional.of(() -> data);
    }

    @Override
    public <R> LazyOptional<R> getCapability(
        Capability<R> requestedCapability,
        @Nullable Direction side
    ) {
        return capability.orEmpty(requestedCapability, optional);
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.savePersistentData();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        data.loadPersistentData(
            Objects.requireNonNull(tag, "tag is null")
        );
    }

    public void invalidate() {
        optional.invalidate();
    }
}
