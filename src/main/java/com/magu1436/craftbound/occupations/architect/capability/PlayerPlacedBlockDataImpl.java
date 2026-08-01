package com.magu1436.craftbound.occupations.architect.capability;

import java.util.Objects;

import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * BlockPosをlongへ圧縮して保持する設置履歴の実装。
 */
public final class PlayerPlacedBlockDataImpl
    implements PlayerPlacedBlockData {

    private static final int DATA_VERSION = 1;
    private static final String VERSION_KEY = "Version";
    private static final String POSITIONS_KEY = "Positions";

    private final LongOpenHashSet positions = new LongOpenHashSet();

    @Override
    public boolean contains(BlockPos pos) {
        return positions.contains(
            Objects.requireNonNull(pos, "pos is null").asLong()
        );
    }

    @Override
    public boolean add(BlockPos pos) {
        return positions.add(
            Objects.requireNonNull(pos, "pos is null").asLong()
        );
    }

    @Override
    public boolean remove(BlockPos pos) {
        return positions.remove(
            Objects.requireNonNull(pos, "pos is null").asLong()
        );
    }

    @Override
    public int removeAll(LongCollection packedPositions) {
        Objects.requireNonNull(
            packedPositions,
            "packed positions is null"
        );

        int originalSize = positions.size();
        positions.removeAll(packedPositions);
        return originalSize - positions.size();
    }

    @Override
    public long[] getPositions() {
        return positions.toLongArray();
    }

    @Override
    public CompoundTag savePersistentData() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(VERSION_KEY, DATA_VERSION);
        tag.putLongArray(POSITIONS_KEY, positions.toLongArray());
        return tag;
    }

    @Override
    public void loadPersistentData(CompoundTag tag) {
        Objects.requireNonNull(tag, "tag is null");
        positions.clear();

        if (!tag.contains(VERSION_KEY, Tag.TAG_INT)
            || tag.getInt(VERSION_KEY) != DATA_VERSION
            || !tag.contains(POSITIONS_KEY, Tag.TAG_LONG_ARRAY)) {
            return;
        }

        for (long packedPos : tag.getLongArray(POSITIONS_KEY)) {
            positions.add(packedPos);
        }
    }
}
