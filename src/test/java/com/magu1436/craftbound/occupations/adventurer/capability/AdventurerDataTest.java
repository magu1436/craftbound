package com.magu1436.craftbound.occupations.adventurer.capability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AdventurerDataTest {

    @Test
    void representsDeathlineCrossingOwnershipAsLevelOne() {
        AdventurerData data = new AdventurerData();

        assertEquals(0, data.getDeathlineCrossingLevel());
        assertFalse(data.hasDeathlineCrossing());

        data.getDeathlineCrossingLevelState().setStageActive(
            1,
            true
        );

        assertEquals(1, data.getDeathlineCrossingLevel());
        assertTrue(data.hasDeathlineCrossing());
    }

    @Test
    void persistsLastActivationDayButNotProtection() {
        AdventurerData original = new AdventurerData();
        original.recordDeathlineCrossingActivation(42L);
        original.startDeathlineCrossingProtection(100L, 60);

        AdventurerData restored = new AdventurerData();
        restored.loadPersistentData(original.savePersistentData());

        assertEquals(
            42L,
            restored.getLastDeathlineCrossingActivationDay()
        );
        assertFalse(restored.isDeathlineCrossingProtected(100L));
    }

    @Test
    void copiesOwnershipAndActivationDayButNotProtectionOnDeath() {
        AdventurerData original = new AdventurerData();
        original.getDeathlineCrossingLevelState().setStageActive(
            1,
            true
        );
        original.recordDeathlineCrossingActivation(42L);
        original.startDeathlineCrossingProtection(100L, 60);

        AdventurerData replacement = new AdventurerData();
        replacement.copyOnDeathFrom(original);

        assertTrue(replacement.hasDeathlineCrossing());
        assertEquals(
            42L,
            replacement.getLastDeathlineCrossingActivationDay()
        );
        assertFalse(
            replacement.isDeathlineCrossingProtected(100L)
        );
    }
}
