package com.magu1436.craftbound.occupations.foodproducer.ranch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class RanchRegistrationRosterTest {

    @Test
    void keepsRegisteredAnimalsStableWhenOverflowOrderChanges() {
        RanchRegistrationRoster roster = new RanchRegistrationRoster();
        List<UUID> managed = java.util.stream.IntStream.range(0, 8)
                .mapToObj(index -> UUID.nameUUIDFromBytes(("managed-" + index).getBytes()))
                .toList();
        UUID overflow = UUID.nameUUIDFromBytes("overflow".getBytes());

        managed.forEach(id -> assertTrue(roster.register(id, 8)));
        assertFalse(roster.register(overflow, 8));

        assertEquals(managed, roster.ids());
        assertFalse(roster.contains(overflow));
    }

    @Test
    void reservesSlotUntilThirtySecondGraceExpires() {
        RanchRegistrationRoster roster = new RanchRegistrationRoster();
        UUID animal = UUID.randomUUID();
        roster.register(animal, 8);
        roster.markOutside(animal, 100L);

        assertFalse(roster.graceExpired(animal, 699L, 600L));
        assertTrue(roster.graceExpired(animal, 700L, 600L));
    }

    @Test
    void returningInsideCancelsPendingRelease() {
        RanchRegistrationRoster roster = new RanchRegistrationRoster();
        UUID animal = UUID.randomUUID();
        roster.register(animal, 8);
        roster.markOutside(animal, 100L);
        roster.markInside(animal);

        assertEquals(RanchRegistrationRoster.INSIDE, roster.outsideSince(animal));
        assertFalse(roster.graceExpired(animal, 10_000L, 600L));
    }

    @Test
    void releasedSlotCanBeFilledWithoutChangingOtherRegistrations() {
        RanchRegistrationRoster roster = new RanchRegistrationRoster();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID replacement = UUID.randomUUID();
        roster.register(first, 2);
        roster.register(second, 2);

        assertTrue(roster.release(first));
        assertTrue(roster.register(replacement, 2));
        assertEquals(List.of(second, replacement), roster.ids());
    }

    @Test
    void increasingCapacityAddsSlotsWithoutReplacingExistingAnimals() {
        RanchRegistrationRoster roster = new RanchRegistrationRoster();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        roster.register(first, 1);

        assertTrue(roster.register(second, 2));
        assertEquals(List.of(first, second), roster.ids());
    }

    @Test
    void loadedRosterPreservesRegistrationOrderAndOutsideGraceStart() {
        RanchRegistrationRoster roster = new RanchRegistrationRoster();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        roster.load(first, RanchRegistrationRoster.INSIDE);
        roster.load(second, 1_200L);

        assertEquals(List.of(first, second), roster.ids());
        assertEquals(1_200L, roster.outsideSince(second));
        assertFalse(roster.graceExpired(second, 1_799L, 600L));
        assertTrue(roster.graceExpired(second, 1_800L, 600L));
    }
}
