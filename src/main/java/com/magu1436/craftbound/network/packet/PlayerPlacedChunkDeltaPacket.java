package com.magu1436.craftbound.network.packet;

import java.util.function.Supplier;

import com.magu1436.craftbound.occupations.architect.client.ClientPlayerPlacedBlockPacketHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record PlayerPlacedChunkDeltaPacket(
    ResourceLocation dimension,
    ChunkPos chunkPos,
    long[] added,
    long[] removed
) {
    public PlayerPlacedChunkDeltaPacket {
        added = added.clone();
        removed = removed.clone();
    }

    public static void encode(
        PlayerPlacedChunkDeltaPacket packet,
        FriendlyByteBuf buffer
    ) {
        buffer.writeResourceLocation(packet.dimension());
        buffer.writeLong(packet.chunkPos().toLong());
        buffer.writeLongArray(packet.added());
        buffer.writeLongArray(packet.removed());
    }

    public static PlayerPlacedChunkDeltaPacket decode(
        FriendlyByteBuf buffer
    ) {
        return new PlayerPlacedChunkDeltaPacket(
            buffer.readResourceLocation(),
            new ChunkPos(buffer.readLong()),
            buffer.readLongArray(),
            buffer.readLongArray()
        );
    }

    public static void handle(
        PlayerPlacedChunkDeltaPacket packet,
        Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
            Dist.CLIENT,
            () -> () -> ClientPlayerPlacedBlockPacketHandler.applyDelta(
                packet.dimension(),
                packet.chunkPos(),
                packet.added(),
                packet.removed()
            )
        ));
        context.setPacketHandled(true);
    }
}
