package com.magu1436.craftbound.network.packet;

import com.magu1436.craftbound.occupations.blacksmith.carving.CarvingPacketHandler;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record CarvingStrokeRequestPacket(UUID sessionId, long sequence, UUID strokeId,
    double startX, double startY, double endX, double endY, boolean excludeStart) {
    public static void encode(CarvingStrokeRequestPacket p, FriendlyByteBuf b) {
        b.writeUUID(p.sessionId); b.writeVarLong(p.sequence); b.writeUUID(p.strokeId);
        b.writeDouble(p.startX); b.writeDouble(p.startY); b.writeDouble(p.endX); b.writeDouble(p.endY); b.writeBoolean(p.excludeStart);
    }
    public static CarvingStrokeRequestPacket decode(FriendlyByteBuf b) {
        return new CarvingStrokeRequestPacket(b.readUUID(), b.readVarLong(), b.readUUID(),
            b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readBoolean());
    }
    public static void handle(CarvingStrokeRequestPacket p, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get(); ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> { if (sender != null) CarvingPacketHandler.stroke(sender, p); }); context.setPacketHandled(true);
    }
}
