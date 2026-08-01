package com.magu1436.craftbound.occupations.architect.capability;

import it.unimi.dsi.fastutil.longs.LongCollection;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/**
 * チャンク内にあるプレイヤー設置ブロックの座標集合。
 */
public interface PlayerPlacedBlockData {

    boolean contains(BlockPos pos);

    boolean add(BlockPos pos);

    boolean remove(BlockPos pos);

    int removeAll(LongCollection packedPositions);

    long[] getPositions();

    CompoundTag savePersistentData();

    void loadPersistentData(CompoundTag tag);
}
