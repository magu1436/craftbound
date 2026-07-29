package com.magu1436.craftbound.network.packet;

import java.util.function.Supplier;

import com.magu1436.craftbound.occupations.adventurer.events.EmergencyEvasionService;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * クライアントからサーバーへ緊急回避の使用要求を送るパケット。
 */
public final class EmergencyEvasionRequestPacket {

    public static void encode(
        EmergencyEvasionRequestPacket packet,
        FriendlyByteBuf buffer
    ) {
    }

    public static EmergencyEvasionRequestPacket decode(
        FriendlyByteBuf buffer
    ) {
        return new EmergencyEvasionRequestPacket();
    }

    public static void handle(
        EmergencyEvasionRequestPacket packet,
        Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();

        context.enqueueWork(() -> {
            if (sender != null) {
                EmergencyEvasionService.tryEvade(sender);
            }
        });
        context.setPacketHandled(true);
    }
}
