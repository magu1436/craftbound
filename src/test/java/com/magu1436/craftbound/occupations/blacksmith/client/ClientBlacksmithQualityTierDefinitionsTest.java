package com.magu1436.craftbound.occupations.blacksmith.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.magu1436.craftbound.occupations.blacksmith.quality.QualityTierDefinition;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClientBlacksmithQualityTierDefinitionsTest {
    @Test
    void replacesSnapshotWithImmutableCopy() {
        List<QualityTierDefinition> source = new ArrayList<>(List.of(
            new QualityTierDefinition("all", 0, 100, "quality.all")
        ));

        ClientBlacksmithQualityTierDefinitions.replace(source);
        source.clear();

        assertEquals("all", ClientBlacksmithQualityTierDefinitions.resolve(50).orElseThrow().id());
        assertThrows(UnsupportedOperationException.class,
            () -> ClientBlacksmithQualityTierDefinitions.all().clear());
    }
}
