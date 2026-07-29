package com.magu1436.craftbound.occupations.adventurer.events;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class FireDurationTrackerTest {

    private static final UUID PLAYER_ID =
        UUID.fromString("67dfcd1d-cd8e-4775-87f8-bef74a1da290");

    private final FireDurationTracker tracker =
        new FireDurationTracker();

    @Test
    void doesNotTreatUninitializedDurationAsUpdate() {
        assertFalse(tracker.isDurationUpdated(PLAYER_ID, 100));
    }

    @Test
    void doesNotTreatNormalDecreaseAsUpdate() {
        tracker.initialize(PLAYER_ID, 100);

        assertFalse(tracker.isDurationUpdated(PLAYER_ID, 99));
    }

    @Test
    void detectsDurationRefresh() {
        tracker.initialize(PLAYER_ID, 100);

        assertTrue(tracker.isDurationUpdated(PLAYER_ID, 100));
    }

    @Test
    void detectsDurationExtension() {
        tracker.initialize(PLAYER_ID, 100);

        assertTrue(tracker.isDurationUpdated(PLAYER_ID, 120));
    }

    @Test
    void detectsIgnitionAfterExtinguishing() {
        tracker.initialize(PLAYER_ID, 0);

        assertTrue(tracker.isDurationUpdated(PLAYER_ID, 100));
    }

    @Test
    void usesRecordedReducedDurationForNextTick() {
        tracker.initialize(PLAYER_ID, 100);
        tracker.record(PLAYER_ID, 60);

        assertFalse(tracker.isDurationUpdated(PLAYER_ID, 59));
    }

    @Test
    void stopsTrackingRemovedPlayer() {
        tracker.initialize(PLAYER_ID, 100);
        tracker.remove(PLAYER_ID);

        assertFalse(tracker.isTracking(PLAYER_ID));
        assertFalse(tracker.isDurationUpdated(PLAYER_ID, 100));
    }

    @Test
    void rejectsNegativeFireTicks() {
        assertThrows(
            IllegalArgumentException.class,
            () -> tracker.initialize(PLAYER_ID, -1)
        );
    }
}
