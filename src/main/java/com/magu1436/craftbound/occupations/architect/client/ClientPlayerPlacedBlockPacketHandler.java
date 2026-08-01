package com.magu1436.craftbound.occupations.architect.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

/**
 * 現在のクライアントディメンションに属する同期だけを反映する。
 */
public final class ClientPlayerPlacedBlockPacketHandler {

    private ClientPlayerPlacedBlockPacketHandler() {
    }

    public static void applySnapshot(
        ResourceLocation dimension,
        ChunkPos chunkPos,
        long[] positions
    ) {
        if (isCurrentDimension(dimension)) {
            ClientPlayerPlacedBlockCache.replace(chunkPos, positions);
        }
    }

    public static void applyDelta(
        ResourceLocation dimension,
        ChunkPos chunkPos,
        long[] added,
        long[] removed
    ) {
        if (isCurrentDimension(dimension)) {
            ClientPlayerPlacedBlockCache.applyDelta(
                chunkPos,
                added,
                removed
            );
        }
    }

    public static void forget(
        ResourceLocation dimension,
        ChunkPos chunkPos
    ) {
        if (isCurrentDimension(dimension)) {
            ClientPlayerPlacedBlockCache.removeChunk(chunkPos);
        }
    }

    private static boolean isCurrentDimension(
        ResourceLocation dimension
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level != null
            && minecraft.level.dimension().location().equals(dimension);
    }
}
