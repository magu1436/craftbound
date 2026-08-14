package com.magu1436.craftbound.network.packet;

import com.magu1436.craftbound.client.forging.ForgingClientSessionState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.resources.ResourceLocation;

public record ForgingFeedbackPacket(Status status, ResourceLocation localSoundId,
    float localVolume, float localPitch) {
    public ForgingFeedbackPacket(Status status) { this(status, null, 0.0F, 0.0F); }
    public static void encode(ForgingFeedbackPacket p, FriendlyByteBuf b) {
        b.writeEnum(p.status); b.writeBoolean(p.localSoundId != null);
        if (p.localSoundId != null) { b.writeResourceLocation(p.localSoundId); b.writeFloat(p.localVolume); b.writeFloat(p.localPitch); }
    }
    public static ForgingFeedbackPacket decode(FriendlyByteBuf b) {
        Status status = b.readEnum(Status.class);
        return b.readBoolean() ? new ForgingFeedbackPacket(status, b.readResourceLocation(), b.readFloat(), b.readFloat())
            : new ForgingFeedbackPacket(status);
    }
    public static void handle(ForgingFeedbackPacket p, Supplier<NetworkEvent.Context> c) {
        NetworkEvent.Context context = c.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> ForgingClientSessionState.feedback(p)));
        context.setPacketHandled(true);
    }
    public enum Status { STRIKE_ACCEPTED, DANGER, INSTINCT, COMPLETED, FAILED, ERROR }
}
