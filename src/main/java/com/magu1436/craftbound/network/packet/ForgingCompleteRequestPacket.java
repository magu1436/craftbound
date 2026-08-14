package com.magu1436.craftbound.network.packet;

import com.magu1436.craftbound.occupations.blacksmith.forging.ForgingPacketHandler;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record ForgingCompleteRequestPacket(UUID sessionId, long sequence) {
    public static void encode(ForgingCompleteRequestPacket p, FriendlyByteBuf b) { b.writeUUID(p.sessionId); b.writeVarLong(p.sequence); }
    public static ForgingCompleteRequestPacket decode(FriendlyByteBuf b) { return new ForgingCompleteRequestPacket(b.readUUID(), b.readVarLong()); }
    public static void handle(ForgingCompleteRequestPacket p, Supplier<NetworkEvent.Context> c) {
        NetworkEvent.Context context = c.get(); ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> { if (sender != null) ForgingPacketHandler.complete(sender, p.sessionId, p.sequence); });
        context.setPacketHandled(true);
    }
}
