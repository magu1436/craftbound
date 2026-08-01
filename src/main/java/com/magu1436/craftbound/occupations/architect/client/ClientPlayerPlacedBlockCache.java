package com.magu1436.craftbound.occupations.architect.client;

import java.util.HashMap;
import java.util.Map;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

/**
 * サーバーから同期された設置履歴をチャンク単位で保持する。
 */
public final class ClientPlayerPlacedBlockCache {
    private static final Map<Long, LongOpenHashSet> POSITIONS_BY_CHUNK =
        new HashMap<>();

    private ClientPlayerPlacedBlockCache() {
    }

    public static boolean contains(BlockPos pos) {
        LongOpenHashSet positions = POSITIONS_BY_CHUNK.get(
            ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4)
        );
        return positions != null && positions.contains(pos.asLong());
    }

    public static void replace(ChunkPos chunkPos, long[] positions) {
        POSITIONS_BY_CHUNK.put(
            chunkPos.toLong(),
            new LongOpenHashSet(positions)
        );
    }

    public static void applyDelta(
        ChunkPos chunkPos,
        long[] added,
        long[] removed
    ) {
        long chunkKey = chunkPos.toLong();
        LongOpenHashSet positions = POSITIONS_BY_CHUNK.computeIfAbsent(
            chunkKey,
            ignored -> new LongOpenHashSet()
        );
        positions.addAll(new LongOpenHashSet(added));
        positions.removeAll(new LongOpenHashSet(removed));
    }

    public static void removeChunk(ChunkPos chunkPos) {
        POSITIONS_BY_CHUNK.remove(chunkPos.toLong());
    }

    public static void clear() {
        POSITIONS_BY_CHUNK.clear();
    }
}
