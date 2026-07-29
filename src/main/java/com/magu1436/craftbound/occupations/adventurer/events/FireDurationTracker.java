package com.magu1436.craftbound.occupations.adventurer.events;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * オンライン中のプレイヤーについて、前回の炎上時間を保持する。
 */
final class FireDurationTracker {

    private final Map<UUID, FireDurationState> states = new HashMap<>();

    public void initialize(UUID playerId, int fireTicks) {
        validate(playerId, fireTicks);
        states.put(playerId, new FireDurationState(fireTicks));
    }

    public boolean isTracking(UUID playerId) {
        return states.containsKey(
            Objects.requireNonNull(playerId, "player id is null")
        );
    }

    public boolean isDurationUpdated(UUID playerId, int currentFireTicks) {
        validate(playerId, currentFireTicks);

        FireDurationState state = states.get(playerId);

        if (state == null) {
            return false;
        }

        int expectedFireTicks = Math.max(
            state.previousFireTicks - 1,
            0
        );

        return currentFireTicks > expectedFireTicks;
    }

    public void record(UUID playerId, int fireTicks) {
        validate(playerId, fireTicks);

        FireDurationState state = states.get(playerId);

        if (state == null) {
            initialize(playerId, fireTicks);
            return;
        }

        state.previousFireTicks = fireTicks;
    }

    public void remove(UUID playerId) {
        states.remove(
            Objects.requireNonNull(playerId, "player id is null")
        );
    }

    private static void validate(UUID playerId, int fireTicks) {
        Objects.requireNonNull(playerId, "player id is null");

        if (fireTicks < 0) {
            throw new IllegalArgumentException(
                "fire ticks must be greater than or equal to 0"
            );
        }
    }

    private static final class FireDurationState {
        private int previousFireTicks;

        private FireDurationState(int previousFireTicks) {
            this.previousFireTicks = previousFireTicks;
        }
    }
}
