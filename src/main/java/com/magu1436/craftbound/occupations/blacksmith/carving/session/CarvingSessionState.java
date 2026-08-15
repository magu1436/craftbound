package com.magu1436.craftbound.occupations.blacksmith.carving.session;

import java.util.Objects;
import java.util.UUID;

public final class CarvingSessionState {
    private final UUID sessionId;
    private final UUID activePlayerId;
    private final UUID processId;
    private long lastSequence;
    private long lastHeartbeatTick;
    private Long disconnectedAtTick;

    public CarvingSessionState(UUID sessionId, UUID activePlayerId, UUID processId, long now) {
        this.sessionId = Objects.requireNonNull(sessionId);
        this.activePlayerId = Objects.requireNonNull(activePlayerId);
        this.processId = Objects.requireNonNull(processId);
        this.lastHeartbeatTick = now;
    }
    public UUID sessionId() { return sessionId; }
    public UUID activePlayerId() { return activePlayerId; }
    public UUID processId() { return processId; }
    public long lastSequence() { return lastSequence; }
    public long lastHeartbeatTick() { return lastHeartbeatTick; }
    public Long disconnectedAtTick() { return disconnectedAtTick; }
    public boolean acceptSequence(long sequence) {
        if (sequence <= lastSequence) return false;
        lastSequence = sequence; return true;
    }
    public void heartbeat(long tick) { lastHeartbeatTick = tick; disconnectedAtTick = null; }
    public void markDisconnected(long tick) { if (disconnectedAtTick == null) disconnectedAtTick = tick; }
}
