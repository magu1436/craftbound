package com.magu1436.craftbound.occupations.architect;

import java.util.Objects;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import com.magu1436.craftbound.occupations.architect.capability.ConstructionChunkDataAccess;
import com.magu1436.craftbound.occupations.architect.network.PlayerPlacedBlockSync;

/**
 * プレイヤーによる設置と、その後のブロック削除を一元管理する。
 */
public final class ConstructionTrackingService {

    private ConstructionTrackingService() {
    }

    public static void onPlaced(
        ServerPlayer player,
        ServerLevel level,
        BlockPos pos,
        BlockState placedState
    ) {
        Objects.requireNonNull(player, "player is null");
        Objects.requireNonNull(level, "level is null");
        Objects.requireNonNull(pos, "pos is null");
        Objects.requireNonNull(placedState, "placed state is null");

        if (!ConstructionChunkDataAccess.markPlayerPlaced(level, pos)) {
            return;
        }

        LevelChunk chunk = getLoadedChunk(level, pos);
        if (chunk != null) {
            PlayerPlacedBlockSync.sendAdded(
                level,
                chunk,
                singlePosition(pos)
            );
        }
    }

    public static void onRemoved(ServerLevel level, BlockPos pos) {
        Objects.requireNonNull(level, "level is null");
        Objects.requireNonNull(pos, "pos is null");

        boolean playerPlacedCleared =
            ConstructionChunkDataAccess.clearPlayerPlaced(level, pos);
        ConstructionChunkDataAccess.removePending(level, pos);

        if (!playerPlacedCleared) {
            return;
        }

        LevelChunk chunk = getLoadedChunk(level, pos);
        if (chunk != null) {
            PlayerPlacedBlockSync.sendRemoved(
                level,
                chunk,
                singlePosition(pos)
            );
        }
    }

    public static boolean isPlayerPlaced(
        ServerLevel level,
        BlockPos pos,
        BlockState currentState
    ) {
        Objects.requireNonNull(currentState, "current state is null");
        if (currentState.isAir()) {
            onRemoved(level, pos);
            return false;
        }
        return ConstructionChunkDataAccess.isPlayerPlaced(level, pos);
    }

    private static LevelChunk getLoadedChunk(
        ServerLevel level,
        BlockPos pos
    ) {
        return level.getChunkSource().getChunkNow(
            pos.getX() >> 4,
            pos.getZ() >> 4
        );
    }

    private static LongArrayList singlePosition(BlockPos pos) {
        LongArrayList positions = new LongArrayList(1);
        positions.add(pos.asLong());
        return positions;
    }
}
