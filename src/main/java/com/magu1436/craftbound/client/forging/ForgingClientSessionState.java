package com.magu1436.craftbound.client.forging;

import com.magu1436.craftbound.network.packet.ForgingFeedbackPacket;
import com.magu1436.craftbound.network.packet.ForgingSessionSyncPacket;

public final class ForgingClientSessionState {
    private static ForgingSessionSyncPacket session;
    private static ForgingFeedbackPacket.Status feedback;

    private ForgingClientSessionState() {}

    public static void update(ForgingSessionSyncPacket packet) { session = packet; }
    public static void feedback(ForgingFeedbackPacket packet) { feedback = packet.status(); }
    public static ForgingSessionSyncPacket session() { return session; }
    public static ForgingFeedbackPacket.Status lastFeedback() { return feedback; }
    public static void clear() { session = null; feedback = null; }
}
