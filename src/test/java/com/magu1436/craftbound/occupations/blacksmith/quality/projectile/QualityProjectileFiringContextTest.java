package com.magu1436.craftbound.occupations.blacksmith.quality.projectile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class QualityProjectileFiringContextTest {
    @AfterEach
    void clearContext() {
        QualityProjectileFiringContext.clearAll();
    }

    @Test
    void returnsQualityRepeatedlyForMultipleProjectilesInSameTick() {
        UUID shooterId = UUID.randomUUID();
        QualityProjectileFiringContext.record(shooterId, 80, 100L);

        assertEquals(80, QualityProjectileFiringContext.find(shooterId, 100L).orElseThrow());
        assertEquals(80, QualityProjectileFiringContext.find(shooterId, 100L).orElseThrow());
    }

    @Test
    void rejectsAndPrunesContextFromAnotherTick() {
        UUID shooterId = UUID.randomUUID();
        QualityProjectileFiringContext.record(shooterId, 80, 100L);

        assertTrue(QualityProjectileFiringContext.find(shooterId, 101L).isEmpty());
        assertTrue(QualityProjectileFiringContext.find(shooterId, 100L).isEmpty());
    }

    @Test
    void cleanupRemovesShooterEntry() {
        UUID shooterId = UUID.randomUUID();
        QualityProjectileFiringContext.record(shooterId, 80, 100L);

        QualityProjectileFiringContext.clear(shooterId);

        assertTrue(QualityProjectileFiringContext.find(shooterId, 100L).isEmpty());
    }

    @Test
    void rejectsQualityOutsideSharedRange() {
        assertThrows(IllegalArgumentException.class,
            () -> QualityProjectileFiringContext.record(UUID.randomUUID(), 101, 100L));
    }
}
