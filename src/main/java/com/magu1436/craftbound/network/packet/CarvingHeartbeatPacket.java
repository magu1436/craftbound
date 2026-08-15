package com.magu1436.craftbound.network.packet;

import com.magu1436.craftbound.occupations.blacksmith.carving.CarvingPacketHandler;
import java.util.UUID; import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf; import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record CarvingHeartbeatPacket(UUID sessionId, long sequence) {
    public static void encode(CarvingHeartbeatPacket p, FriendlyByteBuf b) { b.writeUUID(p.sessionId); b.writeVarLong(p.sequence); }
    public static CarvingHeartbeatPacket decode(FriendlyByteBuf b) { return new CarvingHeartbeatPacket(b.readUUID(), b.readVarLong()); }
    public static void handle(CarvingHeartbeatPacket p, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context c = supplier.get(); ServerPlayer sender = c.getSender();
        c.enqueueWork(() -> { if (sender != null) CarvingPacketHandler.heartbeat(sender, p); }); c.setPacketHandled(true);
    }
}
