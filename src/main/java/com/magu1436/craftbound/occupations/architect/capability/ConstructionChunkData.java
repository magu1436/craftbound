package com.magu1436.craftbound.occupations.architect.capability;

import java.util.Optional;

import it.unimi.dsi.fastutil.ints.IntCollection;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/**
 * 建築に関する座標状態を保持するチャンク単位のデータ。
 */
public interface ConstructionChunkData {

    boolean isPlayerPlaced(BlockPos pos);

    boolean markPlayerPlaced(BlockPos pos);

    boolean clearPlayerPlaced(BlockPos pos);

    int clearAllPlayerPlaced(IntCollection localPositions);

    boolean isXpRewarded(BlockPos pos);

    boolean tryMarkXpRewarded(BlockPos pos);

    Optional<PendingConstruction> getPending(BlockPos pos);

    void putPending(BlockPos pos, PendingConstruction pending);

    boolean removePending(BlockPos pos);

    int[] getPlayerPlacedPositions();

    CompoundTag savePersistentData();

    void loadPersistentData(CompoundTag tag);
}
