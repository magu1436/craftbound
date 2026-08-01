package com.magu1436.craftbound.occupations.architect.events;

import java.util.IdentityHashMap;
import java.util.Map;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import com.magu1436.craftbound.occupations.architect.capability.ConstructionChunkDataAccess;
import com.magu1436.craftbound.occupations.architect.network.PlayerPlacedBlockSync;

/**
 * 破壊イベントの候補を保持し、tick終了時に成立した削除だけを確定する。
 */
public final class PlayerPlacedBlockRemovalQueue {
    private static final Map<
        ServerLevel,
        Long2ObjectOpenHashMap<Block>
    > PENDING_REMOVALS = new IdentityHashMap<>();

    private PlayerPlacedBlockRemovalQueue() {
    }

    public static void enqueueWithAdjacentBlocks(
        ServerLevel level,
        BlockPos origin
    ) {
        enqueueIfTracked(level, origin);
        for (Direction direction : Direction.values()) {
            enqueueIfTracked(level, origin.relative(direction));
        }
    }

    public static void flush(ServerLevel level) {
        Long2ObjectOpenHashMap<Block> removals =
            PENDING_REMOVALS.remove(level);
        if (removals == null || removals.isEmpty()) {
            return;
        }

        Long2ObjectOpenHashMap<LongArrayList> confirmedByChunk =
            new Long2ObjectOpenHashMap<>();
        for (var entry : removals.long2ObjectEntrySet()) {
            BlockPos pos = BlockPos.of(entry.getLongKey());
            LevelChunk chunk = level.getChunkSource().getChunkNow(
                pos.getX() >> 4,
                pos.getZ() >> 4
            );
            if (chunk == null
                || chunk.getBlockState(pos).getBlock() == entry.getValue()) {
                continue;
            }

            confirmedByChunk
                .computeIfAbsent(
                    chunk.getPos().toLong(),
                    ignored -> new LongArrayList()
                )
                .add(entry.getLongKey());
        }

        for (var entry : confirmedByChunk.long2ObjectEntrySet()) {
            ChunkPos chunkPos = new ChunkPos(entry.getLongKey());
            LevelChunk chunk = level.getChunkSource().getChunkNow(
                chunkPos.x,
                chunkPos.z
            );
            if (chunk == null) {
                continue;
            }

            LongArrayList actuallyRemoved =
                ConstructionChunkDataAccess.clearAllPlayerPlaced(
                    chunk,
                    entry.getValue()
                );
            if (!actuallyRemoved.isEmpty()) {
                PlayerPlacedBlockSync.sendRemoved(
                    level,
                    chunk,
                    actuallyRemoved
                );
            }
        }
    }

    public static void clear(ServerLevel level) {
        PENDING_REMOVALS.remove(level);
    }

    private static void enqueueIfTracked(
        ServerLevel level,
        BlockPos pos
    ) {
        if (!level.hasChunkAt(pos)
            || !ConstructionChunkDataAccess.isPlayerPlaced(level, pos)) {
            return;
        }

        BlockState originalState = level.getBlockState(pos);
        if (originalState.isAir()) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(
                pos.getX() >> 4,
                pos.getZ() >> 4
            );
            if (chunk != null
                && ConstructionChunkDataAccess.clearPlayerPlaced(level, pos)) {
                LongArrayList removed = new LongArrayList(1);
                removed.add(pos.asLong());
                PlayerPlacedBlockSync.sendRemoved(
                    level,
                    chunk,
                    removed
                );
            }
            return;
        }

        PENDING_REMOVALS
            .computeIfAbsent(
                level,
                ignored -> new Long2ObjectOpenHashMap<>()
            )
            .putIfAbsent(pos.asLong(), originalState.getBlock());
    }
}
