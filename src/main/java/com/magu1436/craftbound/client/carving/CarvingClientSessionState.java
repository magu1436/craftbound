package com.magu1436.craftbound.client.carving;

import com.magu1436.craftbound.network.CraftboundNetwork;
import com.magu1436.craftbound.network.packet.*;
import com.magu1436.craftbound.occupations.blacksmith.carving.logic.*;
import java.util.*;

public final class CarvingClientSessionState {
    private static UUID sessionId;
    private static CarvingGrid authoritativeGrid;
    private static CarvingGrid predictedGrid;
    private static double brushRadius;
    private static double removePerPass;
    private static long nextSequence;
    private static final NavigableMap<Long, CarvingStroke> pending = new TreeMap<>();
    private static CarvingFeedbackPacket.Status feedback;
    private static long feedbackSequence;
    private static CarvingSessionSyncPacket session;
    private CarvingClientSessionState() {}

    public static void full(CarvingSessionSyncPacket packet) {
        session = packet; sessionId = packet.sessionId(); authoritativeGrid = packet.grid().copy();
        predictedGrid = packet.grid().copy(); brushRadius = packet.brushRadius(); removePerPass = packet.removePerPass();
        nextSequence = Math.max(nextSequence, packet.ackSequence() + 1L); pending.clear();
    }
    public static void delta(CarvingDeltaSyncPacket packet) {
        if (!Objects.equals(sessionId, packet.sessionId()) || authoritativeGrid == null) return;
        for (CarvingStrokeResult.ChangedCell cell : packet.changedCells())
            authoritativeGrid.set(cell.x(), cell.y(), cell.value());
        pending.headMap(packet.ackSequence(), true).clear();
        predictedGrid = authoritativeGrid.copy();
        CarvingStrokeProcessor processor = new CarvingStrokeProcessor();
        pending.values().forEach(stroke -> processor.apply(predictedGrid, stroke, brushRadius, removePerPass));
        nextSequence = Math.max(nextSequence, packet.ackSequence() + 1L);
    }
    public static void sendStroke(UUID strokeId, CarvingStroke stroke) {
        if (sessionId == null || predictedGrid == null) return;
        long sequence = nextSequence++;
        new CarvingStrokeProcessor().apply(predictedGrid, stroke, brushRadius, removePerPass);
        pending.put(sequence, stroke);
        double size = predictedGrid.size();
        CraftboundNetwork.sendToServer(new CarvingStrokeRequestPacket(sessionId, sequence, strokeId,
            (stroke.startX() + .5D) / size, (stroke.startY() + .5D) / size,
            (stroke.endX() + .5D) / size, (stroke.endY() + .5D) / size, stroke.excludeStart()));
    }
    public static void heartbeat() {
        if (sessionId != null) CraftboundNetwork.sendToServer(new CarvingHeartbeatPacket(sessionId, nextSequence++));
    }
    public static CarvingGrid predictedGrid() { return predictedGrid == null ? null : predictedGrid.copy(); }
    public static UUID sessionId() { return sessionId; }
    public static void feedback(CarvingFeedbackPacket packet) { feedback = packet.status(); feedbackSequence++; }
    public static CarvingFeedbackPacket.Status feedback() { return feedback; }
    public static long feedbackSequence() { return feedbackSequence; }
    public static CarvingSessionSyncPacket session() { return session; }
    public static void clear() { session = null; sessionId = null; authoritativeGrid = null; predictedGrid = null;
        pending.clear(); feedback = null; feedbackSequence = 0; nextSequence = 0; }
}
