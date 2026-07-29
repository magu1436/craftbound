package com.magu1436.craftbound.occupations.adventurer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

class DeathlineCrossingSkillDataTest {
    private static final String CATEGORY_DATA_PATH =
        "data/craftbound/puffish_skills/categories/adventurer/";

    @Test
    void definesSingleStageRewardWithSixRequiredSkills()
        throws IOException {

        JsonObject definitions = loadJson("definitions.json");
        JsonObject definition = definitions.getAsJsonObject(
            "deathline_crossing"
        );

        assertNotNull(definition);
        assertEquals(
            6,
            definition.get("required_skills").getAsInt()
        );

        JsonArray rewards = definition.getAsJsonArray("rewards");
        assertEquals(1, rewards.size());

        JsonObject reward = rewards.get(0).getAsJsonObject();
        assertEquals(
            "craftbound:deathline_crossing_level_reward",
            reward.get("type").getAsString()
        );
        assertEquals(
            1,
            reward
                .getAsJsonObject("data")
                .get("stage")
                .getAsInt()
        );
    }

    @Test
    void definesNonRootDeathlineCrossingNode() throws IOException {
        JsonObject skills = loadJson("skills.json");
        JsonObject skill = skills.getAsJsonObject(
            "deathline_crossing"
        );

        assertNotNull(skill);
        assertEquals(
            "deathline_crossing",
            skill.get("definition").getAsString()
        );
        assertFalse(
            skill.has("root") && skill.get("root").getAsBoolean()
        );
    }

    @Test
    void connectsEveryResistanceStageFourNode() throws IOException {
        Set<String> expectedPrerequisites = Set.of(
            "physical_resistance_4",
            "action_resistance_4",
            "sensory_resistance_4",
            "explosion_damage_reduction_4",
            "projectile_damage_reduction_4",
            "burning_resistance_4"
        );
        JsonArray connections = loadJson("connections.json")
            .getAsJsonObject("normal")
            .getAsJsonArray("unidirectional");
        Set<String> actualPrerequisites = new HashSet<>();

        for (JsonElement element : connections) {
            JsonArray connection = element.getAsJsonArray();

            if (
                "deathline_crossing".equals(
                    connection.get(1).getAsString()
                )
            ) {
                actualPrerequisites.add(
                    connection.get(0).getAsString()
                );
            }
        }

        assertEquals(expectedPrerequisites, actualPrerequisites);
    }

    private static JsonObject loadJson(String fileName)
        throws IOException {

        String resourcePath = CATEGORY_DATA_PATH + fileName;
        InputStream stream = DeathlineCrossingSkillDataTest.class
            .getClassLoader()
            .getResourceAsStream(resourcePath);

        assertNotNull(stream, resourcePath + " is missing");

        try (
            stream;
            InputStreamReader reader = new InputStreamReader(
                stream,
                StandardCharsets.UTF_8
            )
        ) {
            JsonElement element = JsonParser.parseReader(reader);
            assertTrue(element.isJsonObject());
            return element.getAsJsonObject();
        }
    }
}
