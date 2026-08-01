package com.magu1436.craftbound.occupations.architect.events;

import java.util.IdentityHashMap;
import java.util.Map;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import com.magu1436.craftbound.occupations.architect.ConstructionTrackingService;
import com.magu1436.craftbound.occupations.architect.capability.ConstructionChunkDataAccess;

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

            ConstructionTrackingService.onRemoved(level, pos);
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
            ConstructionTrackingService.onRemoved(level, pos);
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
