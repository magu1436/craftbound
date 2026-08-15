package com.magu1436.craftbound.network.packet;

import com.magu1436.craftbound.client.carving.CarvingClientSessionState;
import java.util.function.Supplier; import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist; import net.minecraftforge.fml.DistExecutor; import net.minecraftforge.network.NetworkEvent;

public record CarvingFeedbackPacket(Status status) {
    public static void encode(CarvingFeedbackPacket p, FriendlyByteBuf b) { b.writeEnum(p.status); }
    public static CarvingFeedbackPacket decode(FriendlyByteBuf b) { return new CarvingFeedbackPacket(b.readEnum(Status.class)); }
    public static void handle(CarvingFeedbackPacket p, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context c = supplier.get(); c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> CarvingClientSessionState.feedback(p))); c.setPacketHandled(true);
    }
    public enum Status { ACCEPTED, WARNING, TOOL_BROKEN, BROKEN, COMPLETED, ERROR }
}
