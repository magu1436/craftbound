package com.magu1436.craftbound.occupations.blacksmith.quality;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class QualityTargetRegistryTest {
    @Test
    void createsImmutableSnapshotBeforePublication() {
        Set<String> source = new HashSet<>(Set.of("minecraft:iron_sword"));

        Set<String> snapshot = QualityTargetRegistry.immutableCopy(source);
        source.clear();

        assertEquals(Set.of("minecraft:iron_sword"), snapshot);
        assertThrows(UnsupportedOperationException.class,
            () -> snapshot.add("minecraft:iron_pickaxe"));
    }
}
