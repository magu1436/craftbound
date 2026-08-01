package com.magu1436.craftbound.occupations.architect.capability;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongCollection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import com.magu1436.craftbound.registry.CraftboundCapabilities;

/**
 * 建築チャンクデータの取得とチャンクのdirty化を集約する操作窓口。
 */
public final class ConstructionChunkDataAccess {

    private ConstructionChunkDataAccess() {
    }

    public static boolean isPlayerPlaced(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = getLoadedChunk(level, pos);
        return chunk != null && chunk
            .getCapability(CraftboundCapabilities.CONSTRUCTION_CHUNK_DATA)
            .map(data -> data.isPlayerPlaced(pos))
            .orElse(false);
    }

    public static boolean markPlayerPlaced(ServerLevel level, BlockPos pos) {
        return updatePlayerPlaced(level, pos, true);
    }

    public static boolean clearPlayerPlaced(ServerLevel level, BlockPos pos) {
        return updatePlayerPlaced(level, pos, false);
    }

    public static boolean isXpRewarded(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = getLoadedChunk(level, pos);
        return chunk != null && chunk
            .getCapability(CraftboundCapabilities.CONSTRUCTION_CHUNK_DATA)
            .map(data -> data.isXpRewarded(pos))
            .orElse(false);
    }

    public static boolean tryMarkXpRewarded(
        ServerLevel level,
        BlockPos pos
    ) {
        LevelChunk chunk = getLoadedChunk(level, pos);
        if (chunk == null) {
            return false;
        }
        boolean changed = chunk
            .getCapability(CraftboundCapabilities.CONSTRUCTION_CHUNK_DATA)
            .map(data -> data.tryMarkXpRewarded(pos))
            .orElse(false);
        if (changed) {
            chunk.setUnsaved(true);
        }
        return changed;
    }

    public static Optional<PendingConstruction> getPending(
        ServerLevel level,
        BlockPos pos
    ) {
        LevelChunk chunk = getLoadedChunk(level, pos);
        return chunk == null
            ? Optional.empty()
            : chunk
                .getCapability(CraftboundCapabilities.CONSTRUCTION_CHUNK_DATA)
                .resolve()
                .flatMap(data -> data.getPending(pos));
    }

    public static boolean putPending(
        ServerLevel level,
        BlockPos pos,
        PendingConstruction pending
    ) {
        LevelChunk chunk = getLoadedChunk(level, pos);
        if (chunk == null) {
            return false;
        }
        boolean present = chunk
            .getCapability(CraftboundCapabilities.CONSTRUCTION_CHUNK_DATA)
            .map(data -> {
                data.putPending(pos, pending);
                return true;
            })
            .orElse(false);
        if (present) {
            chunk.setUnsaved(true);
        }
        return present;
    }

    public static boolean removePending(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = getLoadedChunk(level, pos);
        if (chunk == null) {
            return false;
        }
        boolean changed = chunk
            .getCapability(CraftboundCapabilities.CONSTRUCTION_CHUNK_DATA)
            .map(data -> data.removePending(pos))
            .orElse(false);
        if (changed) {
            chunk.setUnsaved(true);
        }
        return changed;
    }

    public static int clearAllPlayerPlaced(
        ServerLevel level,
        LongCollection packedPositions
    ) {
        Objects.requireNonNull(level, "level is null");
        Objects.requireNonNull(packedPositions, "packed positions is null");
        Long2ObjectOpenHashMap<LongArrayList> positionsByChunk =
            groupByChunk(packedPositions);

        int removedCount = 0;
        for (var entry : positionsByChunk.long2ObjectEntrySet()) {
            ChunkPos chunkPos = new ChunkPos(entry.getLongKey());
            LevelChunk chunk = level.getChunkSource().getChunkNow(
                chunkPos.x,
                chunkPos.z
            );
            if (chunk != null) {
                removedCount += clearAllPlayerPlaced(
                    chunk,
                    entry.getValue()
                ).size();
            }
        }
        return removedCount;
    }

    public static long[] getPlayerPlacedPositions(LevelChunk chunk) {
        Objects.requireNonNull(chunk, "chunk is null");
        return chunk
            .getCapability(CraftboundCapabilities.CONSTRUCTION_CHUNK_DATA)
            .map(data -> decodePositions(chunk, data.getPlayerPlacedPositions()))
            .orElseGet(() -> new long[0]);
    }

    public static List<PendingConstructionEntry> getPendingConstructions(
        LevelChunk chunk
    ) {
        Objects.requireNonNull(chunk, "chunk is null");
        List<PendingConstructionEntry> entries = new ArrayList<>();
        chunk.getCapability(CraftboundCapabilities.CONSTRUCTION_CHUNK_DATA)
            .ifPresent(data -> {
                for (int localPosition : data.getPendingPositions()) {
                    BlockPos pos = decodePosition(chunk, localPosition);
                    data.getPending(pos).ifPresent(pending -> entries.add(
                        new PendingConstructionEntry(pos, pending)
                    ));
                }
            });
        return List.copyOf(entries);
    }

    public static LongArrayList markAllPlayerPlaced(
        LevelChunk chunk,
        LongCollection packedPositions
    ) {
        return updateAllPlayerPlaced(chunk, packedPositions, true);
    }

    public static LongArrayList clearAllPlayerPlaced(
        LevelChunk chunk,
        LongCollection packedPositions
    ) {
        return updateAllPlayerPlaced(chunk, packedPositions, false);
    }

    private static boolean updatePlayerPlaced(
        ServerLevel level,
        BlockPos pos,
        boolean mark
    ) {
        LevelChunk chunk = getLoadedChunk(level, pos);
        if (chunk == null) {
            return false;
        }
        boolean changed = chunk
            .getCapability(CraftboundCapabilities.CONSTRUCTION_CHUNK_DATA)
            .map(data -> mark
                ? data.markPlayerPlaced(pos)
                : data.clearPlayerPlaced(pos))
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

    private static LongArrayList updateAllPlayerPlaced(
        LevelChunk chunk,
        LongCollection packedPositions,
        boolean mark
    ) {
        Objects.requireNonNull(chunk, "chunk is null");
        Objects.requireNonNull(packedPositions, "packed positions is null");
        LongArrayList changedPositions = new LongArrayList();
        chunk.getCapability(CraftboundCapabilities.CONSTRUCTION_CHUNK_DATA)
            .ifPresent(data -> {
                for (long packedPos : packedPositions) {
                    BlockPos pos = BlockPos.of(packedPos);
                    boolean changed = mark
                        ? data.markPlayerPlaced(pos)
                        : data.clearPlayerPlaced(pos);
                    if (changed) {
                        changedPositions.add(packedPos);
                    }
                }
            });
        if (!changedPositions.isEmpty()) {
            chunk.setUnsaved(true);
        }
        return changedPositions;
    }

    private static Long2ObjectOpenHashMap<LongArrayList> groupByChunk(
        LongCollection packedPositions
    ) {
        Long2ObjectOpenHashMap<LongArrayList> positionsByChunk =
            new Long2ObjectOpenHashMap<>();
        for (long packedPos : packedPositions) {
            BlockPos pos = BlockPos.of(packedPos);
            long chunkKey = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
            positionsByChunk
                .computeIfAbsent(chunkKey, ignored -> new LongArrayList())
                .add(packedPos);
        }
        return positionsByChunk;
    }

    private static long[] decodePositions(LevelChunk chunk, int[] positions) {
        long[] decoded = new long[positions.length];
        for (int index = 0; index < positions.length; index++) {
            decoded[index] = decodePosition(chunk, positions[index]).asLong();
        }
        return decoded;
    }

    private static BlockPos decodePosition(
        LevelChunk chunk,
        int localPosition
    ) {
        ChunkPos chunkPos = chunk.getPos();
        int x = chunkPos.getMinBlockX() + (localPosition & 15);
        int z = chunkPos.getMinBlockZ() + ((localPosition >> 4) & 15);
        int y = chunk.getMinBuildHeight() + (localPosition >> 8);
        return new BlockPos(x, y, z);
    }
}
