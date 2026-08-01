package com.magu1436.craftbound.occupations.architect.capability;

import java.util.Objects;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongCollection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import com.magu1436.craftbound.registry.CraftboundCapabilities;

/**
 * 設置履歴の取得とチャンクのdirty化を集約する操作窓口。
 */
public final class PlayerPlacedBlockAccess {

    private PlayerPlacedBlockAccess() {
    }

    public static boolean contains(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = getLoadedChunk(level, pos);
        return chunk != null && chunk
            .getCapability(CraftboundCapabilities.PLAYER_PLACED_BLOCKS)
            .map(data -> data.contains(pos))
            .orElse(false);
    }

    public static boolean add(ServerLevel level, BlockPos pos) {
        return update(level, pos, true);
    }

    public static boolean remove(ServerLevel level, BlockPos pos) {
        return update(level, pos, false);
    }

    public static int removeAll(
        ServerLevel level,
        LongCollection packedPositions
    ) {
        Objects.requireNonNull(level, "level is null");
        Objects.requireNonNull(
            packedPositions,
            "packed positions is null"
        );

        Long2ObjectOpenHashMap<LongArrayList> positionsByChunk =
            new Long2ObjectOpenHashMap<>();

        for (long packedPos : packedPositions) {
            BlockPos pos = BlockPos.of(packedPos);
            long chunkKey = ChunkPos.asLong(
                pos.getX() >> 4,
                pos.getZ() >> 4
            );
            positionsByChunk
                .computeIfAbsent(chunkKey, ignored -> new LongArrayList())
                .add(packedPos);
        }

        int removedCount = 0;
        for (var entry : positionsByChunk.long2ObjectEntrySet()) {
            ChunkPos chunkPos = new ChunkPos(entry.getLongKey());
            LevelChunk chunk = level.getChunkSource().getChunkNow(
                chunkPos.x,
                chunkPos.z
            );
            if (chunk == null) {
                continue;
            }

            int removedFromChunk = chunk
                .getCapability(CraftboundCapabilities.PLAYER_PLACED_BLOCKS)
                .map(data -> data.removeAll(entry.getValue()))
                .orElse(0);
            if (removedFromChunk > 0) {
                chunk.setUnsaved(true);
                removedCount += removedFromChunk;
            }
        }

        return removedCount;
    }

    public static long[] getPositions(LevelChunk chunk) {
        Objects.requireNonNull(chunk, "chunk is null");
        return chunk
            .getCapability(CraftboundCapabilities.PLAYER_PLACED_BLOCKS)
            .map(PlayerPlacedBlockData::getPositions)
            .orElseGet(() -> new long[0]);
    }

    private static boolean update(
        ServerLevel level,
        BlockPos pos,
        boolean add
    ) {
        LevelChunk chunk = getLoadedChunk(level, pos);
        if (chunk == null) {
            return false;
        }

        boolean changed = chunk
            .getCapability(CraftboundCapabilities.PLAYER_PLACED_BLOCKS)
            .map(data -> add ? data.add(pos) : data.remove(pos))
            .orElse(false);
        if (changed) {
            chunk.setUnsaved(true);
        }
        return changed;
    }

    private static LevelChunk getLoadedChunk(
        ServerLevel level,
        BlockPos pos
    ) {
        Objects.requireNonNull(level, "level is null");
        Objects.requireNonNull(pos, "pos is null");
        return level.getChunkSource().getChunkNow(
            pos.getX() >> 4,
            pos.getZ() >> 4
        );
    }
}
