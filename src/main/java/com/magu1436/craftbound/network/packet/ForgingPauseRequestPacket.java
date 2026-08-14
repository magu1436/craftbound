package com.magu1436.craftbound.network.packet;

import com.magu1436.craftbound.occupations.blacksmith.forging.ForgingPacketHandler;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record ForgingPauseRequestPacket(UUID sessionId, long sequence, long estimatedServerTick) {
    public static void encode(ForgingPauseRequestPacket p, FriendlyByteBuf b) { b.writeUUID(p.sessionId); b.writeVarLong(p.sequence); b.writeVarLong(p.estimatedServerTick); }
    public static ForgingPauseRequestPacket decode(FriendlyByteBuf b) { return new ForgingPauseRequestPacket(b.readUUID(), b.readVarLong(), b.readVarLong()); }
    public static void handle(ForgingPauseRequestPacket p, Supplier<NetworkEvent.Context> c) {
        NetworkEvent.Context context = c.get(); ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> { if (sender != null) ForgingPacketHandler.pause(sender, p.sessionId, p.sequence, p.estimatedServerTick); });
        context.setPacketHandled(true);
    }
}
