package com.magu1436.craftbound.occupations.architect.network;

import it.unimi.dsi.fastutil.longs.LongCollection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import com.magu1436.craftbound.network.CraftboundNetwork;
import com.magu1436.craftbound.network.packet.ForgetPlayerPlacedChunkPacket;
import com.magu1436.craftbound.network.packet.PlayerPlacedChunkDeltaPacket;
import com.magu1436.craftbound.network.packet.PlayerPlacedChunkSnapshotPacket;
import com.magu1436.craftbound.occupations.architect.capability.PlayerPlacedBlockAccess;

/**
 * 設置履歴のサーバーからクライアントへの同期を集約する。
 */
public final class PlayerPlacedBlockSync {
    private static final long[] EMPTY_POSITIONS = new long[0];

    private PlayerPlacedBlockSync() {
    }

    public static void sendSnapshot(
        ServerPlayer player,
        ServerLevel level,
        LevelChunk chunk
    ) {
        CraftboundNetwork.sendToPlayer(
            player,
            new PlayerPlacedChunkSnapshotPacket(
                level.dimension().location(),
                chunk.getPos(),
                PlayerPlacedBlockAccess.getPositions(chunk)
            )
        );
    }

    public static void sendAdded(
        ServerLevel level,
        LevelChunk chunk,
        LongCollection added
    ) {
        sendDelta(level, chunk, added.toLongArray(), EMPTY_POSITIONS);
    }

    public static void sendRemoved(
        ServerLevel level,
        LevelChunk chunk,
        LongCollection removed
    ) {
        sendDelta(level, chunk, EMPTY_POSITIONS, removed.toLongArray());
    }

    public static void sendForget(
        ServerPlayer player,
        ServerLevel level,
        ChunkPos chunkPos
    ) {
        CraftboundNetwork.sendToPlayer(
            player,
            new ForgetPlayerPlacedChunkPacket(
                level.dimension().location(),
                chunkPos
            )
        );
    }

    private static void sendDelta(
        ServerLevel level,
        LevelChunk chunk,
        long[] added,
        long[] removed
    ) {
        CraftboundNetwork.sendToTrackingChunk(
            chunk,
            new PlayerPlacedChunkDeltaPacket(
                level.dimension().location(),
                chunk.getPos(),
                added,
                removed
            )
        );
    }
}
