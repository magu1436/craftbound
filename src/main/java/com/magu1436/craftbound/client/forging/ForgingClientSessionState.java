package com.magu1436.craftbound.client.forging;

import com.magu1436.craftbound.network.packet.ForgingFeedbackPacket;
import com.magu1436.craftbound.network.packet.ForgingSessionSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.ForgeRegistries;

public final class ForgingClientSessionState {
    private static ForgingSessionSyncPacket session;
    private static ForgingFeedbackPacket.Status feedback;
    private static long receivedClientTick;
    private static long nextSequence;

    private ForgingClientSessionState() {}

    public static void update(ForgingSessionSyncPacket packet) {
        session = packet;
        Minecraft minecraft = Minecraft.getInstance();
        receivedClientTick = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        nextSequence = Math.max(nextSequence, packet.lastAcceptedSequence() + 1L);
    }
    public static void feedback(ForgingFeedbackPacket packet) {
        feedback = packet.status();
        if (packet.status() == ForgingFeedbackPacket.Status.INSTINCT && packet.localSoundId() != null) {
            Minecraft minecraft = Minecraft.getInstance();
            SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(packet.localSoundId());
            if (minecraft.player != null && sound != null) {
                minecraft.player.playSound(sound, packet.localVolume(), packet.localPitch());
            }
        }
    }
    public static ForgingSessionSyncPacket session() { return session; }
    public static ForgingFeedbackPacket.Status lastFeedback() { return feedback; }
    public static long nextSequence() { return nextSequence++; }
    public static long estimatedServerTick() {
        if (session == null) return 0L;
        Minecraft minecraft = Minecraft.getInstance();
        long clientTick = minecraft.level == null ? receivedClientTick : minecraft.level.getGameTime();
        return session.serverGameTime() + clientTick - receivedClientTick;
    }
    public static void clear() { session = null; feedback = null; receivedClientTick = 0L; nextSequence = 0L; }
}
