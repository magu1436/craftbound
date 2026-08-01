package com.magu1436.craftbound.network.packet;

import java.util.function.Supplier;

import com.magu1436.craftbound.occupations.architect.client.ClientPlayerPlacedBlockPacketHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record PlayerPlacedChunkSnapshotPacket(
    ResourceLocation dimension,
    ChunkPos chunkPos,
    long[] positions
) {
    public PlayerPlacedChunkSnapshotPacket {
        positions = positions.clone();
    }

    public static void encode(
        PlayerPlacedChunkSnapshotPacket packet,
        FriendlyByteBuf buffer
    ) {
        buffer.writeResourceLocation(packet.dimension());
        buffer.writeLong(packet.chunkPos().toLong());
        buffer.writeLongArray(packet.positions());
    }

    public static PlayerPlacedChunkSnapshotPacket decode(
        FriendlyByteBuf buffer
    ) {
        return new PlayerPlacedChunkSnapshotPacket(
            buffer.readResourceLocation(),
            new ChunkPos(buffer.readLong()),
            buffer.readLongArray()
        );
    }

    public static void handle(
        PlayerPlacedChunkSnapshotPacket packet,
        Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
            Dist.CLIENT,
            () -> () -> ClientPlayerPlacedBlockPacketHandler.applySnapshot(
                packet.dimension(),
                packet.chunkPos(),
                packet.positions()
            )
        ));
        context.setPacketHandled(true);
    }
}
