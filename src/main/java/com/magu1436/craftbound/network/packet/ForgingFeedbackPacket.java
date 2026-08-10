package com.magu1436.craftbound.network.packet;

import com.magu1436.craftbound.client.forging.ForgingClientSessionState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ForgingFeedbackPacket(Status status) {
    public static void encode(ForgingFeedbackPacket p, FriendlyByteBuf b) { b.writeEnum(p.status); }
    public static ForgingFeedbackPacket decode(FriendlyByteBuf b) { return new ForgingFeedbackPacket(b.readEnum(Status.class)); }
    public static void handle(ForgingFeedbackPacket p, Supplier<NetworkEvent.Context> c) {
        NetworkEvent.Context context = c.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> ForgingClientSessionState.feedback(p)));
        context.setPacketHandled(true);
    }
    public enum Status { STRIKE_ACCEPTED, DANGER, INSTINCT, COMPLETED, FAILED, ERROR }
}
