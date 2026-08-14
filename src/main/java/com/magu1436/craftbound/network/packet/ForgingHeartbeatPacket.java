package com.magu1436.craftbound.network.packet;

import com.magu1436.craftbound.occupations.blacksmith.forging.ForgingPacketHandler;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record ForgingHeartbeatPacket(UUID sessionId, long sequence) {
    public static void encode(ForgingHeartbeatPacket p, FriendlyByteBuf b) { b.writeUUID(p.sessionId); b.writeVarLong(p.sequence); }
    public static ForgingHeartbeatPacket decode(FriendlyByteBuf b) { return new ForgingHeartbeatPacket(b.readUUID(), b.readVarLong()); }
    public static void handle(ForgingHeartbeatPacket p, Supplier<NetworkEvent.Context> c) {
        NetworkEvent.Context context = c.get(); ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> { if (sender != null) ForgingPacketHandler.heartbeat(sender, p.sessionId, p.sequence); });
        context.setPacketHandled(true);
    }
}
