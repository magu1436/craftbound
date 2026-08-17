package com.magu1436.craftbound.network;

import java.util.Optional;

import com.magu1436.craftbound.common.CraftboundUtilities;
import com.magu1436.craftbound.network.packet.EmergencyEvasionRequestPacket;
import com.magu1436.craftbound.network.packet.ForgetPlayerPlacedChunkPacket;
import com.magu1436.craftbound.network.packet.PlayerPlacedChunkDeltaPacket;
import com.magu1436.craftbound.network.packet.PlayerPlacedChunkSnapshotPacket;
import com.magu1436.craftbound.network.packet.ForgingStrikeRequestPacket;
import com.magu1436.craftbound.network.packet.ForgingCompleteRequestPacket;
import com.magu1436.craftbound.network.packet.ForgingPauseRequestPacket;
import com.magu1436.craftbound.network.packet.ForgingHeartbeatPacket;
import com.magu1436.craftbound.network.packet.ForgingSessionSyncPacket;
import com.magu1436.craftbound.network.packet.ForgingFeedbackPacket;
import com.magu1436.craftbound.network.packet.CarvingStrokeRequestPacket;
import com.magu1436.craftbound.network.packet.CarvingHeartbeatPacket;
import com.magu1436.craftbound.network.packet.CarvingSessionSyncPacket;
import com.magu1436.craftbound.network.packet.CarvingDeltaSyncPacket;
import com.magu1436.craftbound.network.packet.CarvingFeedbackPacket;
import com.magu1436.craftbound.network.packet.BlacksmithQualityTierSyncPacket;
import com.magu1436.craftbound.network.packet.BlacksmithQualityPerformanceSyncPacket;

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
    private static final String PROTOCOL_VERSION = "6";

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
            packetId++,
            ForgetPlayerPlacedChunkPacket.class,
            ForgetPlayerPlacedChunkPacket::encode,
            ForgetPlayerPlacedChunkPacket::decode,
            ForgetPlayerPlacedChunkPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(packetId++, ForgingStrikeRequestPacket.class,
            ForgingStrikeRequestPacket::encode, ForgingStrikeRequestPacket::decode,
            ForgingStrikeRequestPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(packetId++, ForgingCompleteRequestPacket.class,
            ForgingCompleteRequestPacket::encode, ForgingCompleteRequestPacket::decode,
            ForgingCompleteRequestPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(packetId++, ForgingPauseRequestPacket.class,
            ForgingPauseRequestPacket::encode, ForgingPauseRequestPacket::decode,
            ForgingPauseRequestPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(packetId++, ForgingHeartbeatPacket.class,
            ForgingHeartbeatPacket::encode, ForgingHeartbeatPacket::decode,
            ForgingHeartbeatPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(packetId++, ForgingSessionSyncPacket.class,
            ForgingSessionSyncPacket::encode, ForgingSessionSyncPacket::decode,
            ForgingSessionSyncPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(packetId++, ForgingFeedbackPacket.class,
            ForgingFeedbackPacket::encode, ForgingFeedbackPacket::decode,
            ForgingFeedbackPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(packetId++, CarvingStrokeRequestPacket.class,
            CarvingStrokeRequestPacket::encode, CarvingStrokeRequestPacket::decode,
            CarvingStrokeRequestPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(packetId++, CarvingHeartbeatPacket.class,
            CarvingHeartbeatPacket::encode, CarvingHeartbeatPacket::decode,
            CarvingHeartbeatPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(packetId++, CarvingSessionSyncPacket.class,
            CarvingSessionSyncPacket::encode, CarvingSessionSyncPacket::decode,
            CarvingSessionSyncPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(packetId++, CarvingDeltaSyncPacket.class,
            CarvingDeltaSyncPacket::encode, CarvingDeltaSyncPacket::decode,
            CarvingDeltaSyncPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(packetId++, CarvingFeedbackPacket.class,
            CarvingFeedbackPacket::encode, CarvingFeedbackPacket::decode,
            CarvingFeedbackPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(packetId++, BlacksmithQualityTierSyncPacket.class,
            BlacksmithQualityTierSyncPacket::encode, BlacksmithQualityTierSyncPacket::decode,
            BlacksmithQualityTierSyncPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(packetId, BlacksmithQualityPerformanceSyncPacket.class,
            BlacksmithQualityPerformanceSyncPacket::encode, BlacksmithQualityPerformanceSyncPacket::decode,
            BlacksmithQualityPerformanceSyncPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));

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

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
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
