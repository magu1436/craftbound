package com.magu1436.craftbound.network;

import java.util.Optional;

import com.magu1436.craftbound.common.CraftboundUtilities;
import com.magu1436.craftbound.network.packet.EmergencyEvasionRequestPacket;

import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Craftboundのネットワークチャンネルとパケットを管理する。
 */
public final class CraftboundNetwork {
    private static final String PROTOCOL_VERSION = "1";

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
            packetId,
            EmergencyEvasionRequestPacket.class,
            EmergencyEvasionRequestPacket::encode,
            EmergencyEvasionRequestPacket::decode,
            EmergencyEvasionRequestPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        registered = true;
    }

    public static void sendEmergencyEvasionRequest() {
        CHANNEL.sendToServer(new EmergencyEvasionRequestPacket());
    }
}
