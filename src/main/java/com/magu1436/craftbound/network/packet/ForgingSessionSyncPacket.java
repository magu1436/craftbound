package com.magu1436.craftbound.network.packet;

import com.magu1436.craftbound.client.forging.ForgingClientSessionState;
import com.magu1436.craftbound.occupations.blacksmith.forging.session.ForgingAssistSnapshot;
import com.magu1436.craftbound.occupations.blacksmith.forging.state.GaugeDirection;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ForgingSessionSyncPacket(UUID sessionId, long lastAcceptedSequence,
    long serverGameTime, double baseGaugeValue, GaugeDirection baseGaugeDirection,
    ForgingAssistSnapshot assistSnapshot) {
    public static void encode(ForgingSessionSyncPacket p, FriendlyByteBuf b) {
        b.writeUUID(p.sessionId); b.writeVarLong(p.lastAcceptedSequence); b.writeVarLong(p.serverGameTime);
        b.writeDouble(p.baseGaugeValue); b.writeEnum(p.baseGaugeDirection);
        b.writeVarInt(p.assistSnapshot.markInterval()); b.writeBoolean(p.assistSnapshot.showCurrentValue());
        b.writeBoolean(p.assistSnapshot.strikeReferenceEnabled());
    }
    public static ForgingSessionSyncPacket decode(FriendlyByteBuf b) {
        return new ForgingSessionSyncPacket(b.readUUID(), b.readVarLong(), b.readVarLong(), b.readDouble(),
            b.readEnum(GaugeDirection.class), new ForgingAssistSnapshot(b.readVarInt(), b.readBoolean(), b.readBoolean()));
    }
    public static void handle(ForgingSessionSyncPacket p, Supplier<NetworkEvent.Context> c) {
        NetworkEvent.Context context = c.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> ForgingClientSessionState.update(p)));
        context.setPacketHandled(true);
    }
}
