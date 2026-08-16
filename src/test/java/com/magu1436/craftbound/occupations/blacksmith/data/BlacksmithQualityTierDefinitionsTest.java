package com.magu1436.craftbound.occupations.blacksmith.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonParser;
import com.magu1436.craftbound.occupations.blacksmith.quality.QualityTierDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;

class BlacksmithQualityTierDefinitionsTest {
    @Test
    void parsesAndSortsCompleteCoverage() {
        List<QualityTierDefinition> tiers = BlacksmithQualityTierDefinitions.parse(JsonParser.parseString("""
            {"tiers":[
              {"id":"high","min":50,"max":100,"translation_key":"quality.high"},
              {"id":"low","min":0,"max":49,"translation_key":"quality.low"}
            ]}
            """));

        assertEquals(List.of("low", "high"), tiers.stream().map(QualityTierDefinition::id).toList());
        assertThrows(UnsupportedOperationException.class, () -> tiers.add(tiers.get(0)));
    }

    @Test
    void rejectsGapOverlapAndDuplicateIds() {
        assertInvalid("""
            {"tiers":[
              {"id":"low","min":0,"max":49,"translation_key":"quality.low"},
              {"id":"high","min":51,"max":100,"translation_key":"quality.high"}
            ]}
            """);
        assertInvalid("""
            {"tiers":[
              {"id":"low","min":0,"max":50,"translation_key":"quality.low"},
              {"id":"high","min":50,"max":100,"translation_key":"quality.high"}
            ]}
            """);
        assertInvalid("""
            {"tiers":[
              {"id":"same","min":0,"max":49,"translation_key":"quality.low"},
              {"id":"same","min":50,"max":100,"translation_key":"quality.high"}
            ]}
            """);
    }

    private static void assertInvalid(String json) {
        assertThrows(RuntimeException.class,
            () -> BlacksmithQualityTierDefinitions.parse(JsonParser.parseString(json)));
    }
}
