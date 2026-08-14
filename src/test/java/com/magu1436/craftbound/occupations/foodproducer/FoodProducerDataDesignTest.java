package com.magu1436.craftbound.occupations.foodproducer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class FoodProducerDataDesignTest {

    private static final String SKILL_BASE =
            "/data/craftbound/puffish_skills/categories/food_producer/";
    private static final String FOOD_RECIPE_BASE =
            "/data/craftbound/craftbound_food_recipes/";

    @Test
    void skillTreeUsesThreeFunctionalRootsAndMatchingDefinitions() {
        JsonObject skills = resource(SKILL_BASE + "skills.json");
        JsonObject definitions = resource(SKILL_BASE + "definitions.json");
        JsonObject connections = resource(SKILL_BASE + "connections.json");

        Set<String> roots = skills.entrySet().stream()
                .filter(entry -> entry.getValue().getAsJsonObject().has("root"))
                .filter(entry -> entry.getValue().getAsJsonObject().get("root").getAsBoolean())
                .map(java.util.Map.Entry::getKey)
                .collect(Collectors.toSet());

        assertEquals(Set.of("farmland_diagnosis", "ranch_management", "basic_processing"), roots);
        assertFalse(skills.has("food_producer_root"));
        assertFalse(definitions.has("food_producer_root"));
        assertEquals(skills.keySet(), definitions.keySet());
        assertTrue(connections.getAsJsonObject("normal").has("unidirectional"));
    }

    @Test
    void onlyFirstPreservedFoodAllowsCreateAutomation() {
        assertRecipePolicy("provisional_dried_fruit_wrap.json", 0, "create_low_quality");
        assertRecipePolicy("provisional_dried_mixed_pack.json", 2, "manual_only");
        assertRecipePolicy("provisional_dried_meat_ration.json", 3, "manual_only");
        assertRecipePolicy("provisional_deluxe_dried_mix.json", 4, "manual_only");
        assertRecipePolicy("provisional_ultimate_ration.json", 5, "manual_only");
    }

    @Test
    void publicEquipmentUsesVanillaRecipesAndTaggedKnife() {
        for (String recipe : Set.of(
                "cooking_knife",
                "cooking_table",
                "hand_mill",
                "drying_rack",
                "cooking_pot"
        )) {
            assertEquals(
                    "minecraft:crafting_shapeless",
                    resource("/data/craftbound/recipes/" + recipe + ".json").get("type").getAsString()
            );
        }

        JsonObject knifeTag = resource("/data/craftbound/tags/items/cooking_knives.json");
        assertTrue(knifeTag.getAsJsonArray("values").asList().stream()
                .anyMatch(value -> value.getAsString().equals("craftbound:cooking_knife")));
    }

    private static void assertRecipePolicy(String file, int rank, String policy) {
        JsonObject recipe = resource(FOOD_RECIPE_BASE + file);
        assertEquals(rank, recipe.get("required_recipe_rank").getAsInt());
        assertEquals(policy, recipe.get("automation_policy").getAsString());
    }

    private static JsonObject resource(String path) {
        InputStream stream = FoodProducerDataDesignTest.class.getResourceAsStream(path);
        assertNotNull(stream, "Missing test resource: " + path);
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (java.io.IOException exception) {
            throw new AssertionError("Failed to read test resource: " + path, exception);
        }
    }
}
