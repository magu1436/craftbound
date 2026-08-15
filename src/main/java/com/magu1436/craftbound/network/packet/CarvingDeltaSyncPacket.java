package com.magu1436.craftbound.network.packet;

import com.magu1436.craftbound.client.carving.CarvingClientSessionState;
import com.magu1436.craftbound.occupations.blacksmith.carving.logic.CarvingStrokeResult;
import java.util.*; import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf; import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor; import net.minecraftforge.network.NetworkEvent;

public record CarvingDeltaSyncPacket(UUID sessionId, long ackSequence,
    List<CarvingStrokeResult.ChangedCell> changedCells) {
    public CarvingDeltaSyncPacket { changedCells = List.copyOf(changedCells); }
    public static void encode(CarvingDeltaSyncPacket p, FriendlyByteBuf b) {
        b.writeUUID(p.sessionId); b.writeVarLong(p.ackSequence); b.writeVarInt(p.changedCells.size());
        p.changedCells.forEach(cell -> { b.writeVarInt(cell.x()); b.writeVarInt(cell.y()); b.writeDouble(cell.value()); });
    }
    public static CarvingDeltaSyncPacket decode(FriendlyByteBuf b) {
        UUID id = b.readUUID(); long ack = b.readVarLong(); int count = b.readVarInt();
        if (count < 0 || count > 4096) throw new IllegalArgumentException("invalid carving delta");
        List<CarvingStrokeResult.ChangedCell> cells = new ArrayList<>(count);
        for (int i = 0; i < count; i++) cells.add(new CarvingStrokeResult.ChangedCell(b.readVarInt(), b.readVarInt(), b.readDouble()));
        return new CarvingDeltaSyncPacket(id, ack, cells);
    }
    public static void handle(CarvingDeltaSyncPacket p, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context c = supplier.get(); c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> CarvingClientSessionState.delta(p))); c.setPacketHandled(true);
    }
}
