package com.magu1436.craftbound.network.packet;

import java.util.function.Supplier;

import com.magu1436.craftbound.occupations.architect.client.ClientPlayerPlacedBlockPacketHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ForgetPlayerPlacedChunkPacket(
    ResourceLocation dimension,
    ChunkPos chunkPos
) {
    public static void encode(
        ForgetPlayerPlacedChunkPacket packet,
        FriendlyByteBuf buffer
    ) {
        buffer.writeResourceLocation(packet.dimension());
        buffer.writeLong(packet.chunkPos().toLong());
    }

    public static ForgetPlayerPlacedChunkPacket decode(
        FriendlyByteBuf buffer
    ) {
        return new ForgetPlayerPlacedChunkPacket(
            buffer.readResourceLocation(),
            new ChunkPos(buffer.readLong())
        );
    }

    public static void handle(
        ForgetPlayerPlacedChunkPacket packet,
        Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
            Dist.CLIENT,
            () -> () -> ClientPlayerPlacedBlockPacketHandler.forget(
                packet.dimension(),
                packet.chunkPos()
            )
        ));
        context.setPacketHandled(true);
    }
}
