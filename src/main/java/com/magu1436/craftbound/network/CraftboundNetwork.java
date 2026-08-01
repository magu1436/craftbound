package com.magu1436.craftbound.network;

import java.util.Optional;

import com.magu1436.craftbound.common.CraftboundUtilities;
import com.magu1436.craftbound.network.packet.EmergencyEvasionRequestPacket;
import com.magu1436.craftbound.network.packet.ForgetPlayerPlacedChunkPacket;
import com.magu1436.craftbound.network.packet.PlayerPlacedChunkDeltaPacket;
import com.magu1436.craftbound.network.packet.PlayerPlacedChunkSnapshotPacket;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Craftboundのネットワークチャンネルとパケットを管理する。
 */
public final class CraftboundNetwork {
    private static final String PROTOCOL_VERSION = "2";

    private static final SimpleChannel CHANNEL =
        NetworkRegistry.newSimpleChannel(
            CraftboundUtilities.createResourceLocation("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
        );

    private static boolean registered;

    private CraftboundNetwork() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }

        int packetId = 0;

        CHANNEL.registerMessage(
            packetId++,
            EmergencyEvasionRequestPacket.class,
            EmergencyEvasionRequestPacket::encode,
            EmergencyEvasionRequestPacket::decode,
            EmergencyEvasionRequestPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        CHANNEL.registerMessage(
            packetId++,
            PlayerPlacedChunkSnapshotPacket.class,
            PlayerPlacedChunkSnapshotPacket::encode,
            PlayerPlacedChunkSnapshotPacket::decode,
            PlayerPlacedChunkSnapshotPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            packetId++,
            PlayerPlacedChunkDeltaPacket.class,
            PlayerPlacedChunkDeltaPacket::encode,
            PlayerPlacedChunkDeltaPacket::decode,
            PlayerPlacedChunkDeltaPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
            packetId,
            ForgetPlayerPlacedChunkPacket.class,
            ForgetPlayerPlacedChunkPacket::encode,
            ForgetPlayerPlacedChunkPacket::decode,
            ForgetPlayerPlacedChunkPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        registered = true;
    }

    public static void sendEmergencyEvasionRequest() {
        CHANNEL.sendToServer(new EmergencyEvasionRequestPacket());
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            packet
        );
    }

    public static void sendToTrackingChunk(
        LevelChunk chunk,
        Object packet
    ) {
        CHANNEL.send(
            PacketDistributor.TRACKING_CHUNK.with(() -> chunk),
            packet
        );
    }
}
