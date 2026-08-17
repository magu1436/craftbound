package com.magu1436.craftbound.occupations.blacksmith.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonParser;
import com.magu1436.craftbound.common.CraftboundUtilities;
import com.magu1436.craftbound.occupations.blacksmith.quality.QualityPerformanceType;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BlacksmithQualityPerformanceDefinitionsTest {
    private static final String VALID_JSON = """
        {
          "schema_version": 1,
          "default_quality": 30,
          "modifiers": {
            "attack_damage": {"evaluator":"craftbound:linear_multiplier","base":0.7,"per_quality":0.006},
            "projectile_damage": {"evaluator":"craftbound:linear_multiplier","base":0.7,"per_quality":0.006},
            "armor": {"evaluator":"craftbound:linear_multiplier","base":0.7,"per_quality":0.006,"round_to":0.5},
            "armor_toughness": {"evaluator":"craftbound:linear_multiplier","base":0.7,"per_quality":0.006,"round_to":0.5},
            "mining_speed": {"evaluator":"craftbound:linear_multiplier","base":0.7,"per_quality":0.006},
            "max_durability": {"evaluator":"craftbound:linear_multiplier","base":0.7,"per_quality":0.006,"rounding":"round","preserve_damage_ratio":true}
          }
        }
        """;

    @Test
    void parsesCompleteImmutableRuntimeDefinition() {
        BlacksmithQualityPerformanceDefinitions.Definition definition =
            BlacksmithQualityPerformanceDefinitions.parse(JsonParser.parseString(VALID_JSON));

        assertEquals(30, definition.defaultQuality());
        assertEquals(6, definition.modifiers().size());
        assertEquals(0.5D, definition.modifiers().get(QualityPerformanceType.ARMOR).roundTo());
        assertThrows(UnsupportedOperationException.class,
            () -> definition.modifiers().clear());
    }

    @Test
    void rejectsInvalidDefinitionValues() {
        assertInvalid(VALID_JSON.replace("craftbound:linear_multiplier", "craftbound:unknown"));
        assertInvalid(VALID_JSON.replace("\"default_quality\": 30", "\"default_quality\": 101"));
        assertInvalid(VALID_JSON.replace("\"round_to\":0.5", "\"round_to\":0.0"));
        assertInvalid(VALID_JSON.replace("\"base\":0.7", "\"base\":\"NaN\""));
        assertInvalid(VALID_JSON.replace(
            "\"attack_damage\":",
            "\"unknown_modifier\":{\"evaluator\":\"craftbound:linear_multiplier\",\"base\":0.7,\"per_quality\":0.006},\"attack_damage\":"
        ));
    }

    @Test
    void invalidReloadKeepsPreviousValidDefinition() {
        BlacksmithQualityPerformanceDefinitions.INSTANCE.apply(
            Map.of(CraftboundUtilities.createResourceLocation("performance"), JsonParser.parseString(VALID_JSON)),
            null,
            null
        );
        BlacksmithQualityPerformanceDefinitions.Definition valid =
            BlacksmithQualityPerformanceDefinitions.current();

        BlacksmithQualityPerformanceDefinitions.INSTANCE.apply(
            Map.of(
                CraftboundUtilities.createResourceLocation("performance"),
                JsonParser.parseString(VALID_JSON.replace("\"default_quality\": 30", "\"default_quality\": -1"))
            ),
            null,
            null
        );

        assertSame(valid, BlacksmithQualityPerformanceDefinitions.current());
    }

    private static void assertInvalid(String json) {
        assertThrows(RuntimeException.class,
            () -> BlacksmithQualityPerformanceDefinitions.parse(JsonParser.parseString(json)));
    }
}
