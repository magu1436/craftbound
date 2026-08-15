package com.magu1436.craftbound.network.packet;

import com.magu1436.craftbound.client.carving.CarvingClientSessionState;
import com.magu1436.craftbound.occupations.blacksmith.carving.logic.CarvingGrid;
import java.util.UUID; import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf; import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor; import net.minecraftforge.network.NetworkEvent;

public record CarvingSessionSyncPacket(UUID sessionId, long ackSequence, double brushRadius,
    double removePerPass, CarvingGrid grid) {
    public static void encode(CarvingSessionSyncPacket p, FriendlyByteBuf b) {
        b.writeUUID(p.sessionId); b.writeVarLong(p.ackSequence); b.writeDouble(p.brushRadius); b.writeDouble(p.removePerPass);
        b.writeVarInt(p.grid.size()); for (double value : p.grid.values()) b.writeDouble(value);
    }
    public static CarvingSessionSyncPacket decode(FriendlyByteBuf b) {
        UUID id = b.readUUID(); long ack = b.readVarLong(); double radius = b.readDouble(), remove = b.readDouble();
        int size = b.readVarInt(); if (size < 1 || size > 64) throw new IllegalArgumentException("invalid carving grid size");
        double[] values = new double[size * size]; for (int i = 0; i < values.length; i++) values[i] = b.readDouble();
        return new CarvingSessionSyncPacket(id, ack, radius, remove, new CarvingGrid(size, values));
    }
    public static void handle(CarvingSessionSyncPacket p, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context c = supplier.get(); c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> CarvingClientSessionState.full(p))); c.setPacketHandled(true);
    }
}
