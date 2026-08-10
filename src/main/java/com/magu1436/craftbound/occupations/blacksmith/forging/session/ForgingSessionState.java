package com.magu1436.craftbound.occupations.blacksmith.forging.session;

import com.magu1436.craftbound.occupations.blacksmith.forging.state.GaugeDirection;
import java.util.Objects;
import java.util.UUID;

public final class ForgingSessionState {
    private final UUID sessionId;
    private final UUID activePlayerId;
    private long lastSequence;
    private final long baseServerTick;
    private final double baseGaugeValue;
    private final GaugeDirection baseGaugeDirection;
    private long lastHeartbeatTick;
    private Long disconnectedAtTick;
    private long lastInstinctWarningTick = Long.MIN_VALUE;
    private ForgingAssistSnapshot assistSnapshot;

    public ForgingSessionState(UUID sessionId, UUID activePlayerId, long baseServerTick,
        double baseGaugeValue, GaugeDirection baseGaugeDirection, long lastHeartbeatTick,
        ForgingAssistSnapshot assistSnapshot) {
        this.sessionId = Objects.requireNonNull(sessionId);
        this.activePlayerId = Objects.requireNonNull(activePlayerId);
        this.baseServerTick = baseServerTick;
        this.baseGaugeValue = baseGaugeValue;
        this.baseGaugeDirection = Objects.requireNonNull(baseGaugeDirection);
        this.lastHeartbeatTick = lastHeartbeatTick;
        this.assistSnapshot = Objects.requireNonNull(assistSnapshot);
    }

    public UUID sessionId() { return sessionId; }
    public UUID activePlayerId() { return activePlayerId; }
    public long lastSequence() { return lastSequence; }
    public long baseServerTick() { return baseServerTick; }
    public double baseGaugeValue() { return baseGaugeValue; }
    public GaugeDirection baseGaugeDirection() { return baseGaugeDirection; }
    public long lastHeartbeatTick() { return lastHeartbeatTick; }
    public Long disconnectedAtTick() { return disconnectedAtTick; }
    public long lastInstinctWarningTick() { return lastInstinctWarningTick; }
    public ForgingAssistSnapshot assistSnapshot() { return assistSnapshot; }

    public boolean acceptSequence(long sequence) {
        if (sequence <= lastSequence) return false;
        lastSequence = sequence;
        return true;
    }

    public void heartbeat(long tick) { lastHeartbeatTick = tick; disconnectedAtTick = null; }
    public void markDisconnected(long tick) { if (disconnectedAtTick == null) disconnectedAtTick = tick; }
    public void markInstinctWarning(long tick) { lastInstinctWarningTick = tick; }
    public void updateAssistSnapshot(ForgingAssistSnapshot snapshot) { assistSnapshot = Objects.requireNonNull(snapshot); }
}
