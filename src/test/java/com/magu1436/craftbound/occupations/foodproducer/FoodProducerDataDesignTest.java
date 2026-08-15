package com.magu1436.craftbound.occupations.foodproducer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.List;
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

    @Test
    void implementedRoleMealsAccumulateOneRankOneAttributePerRank() {
        assertRoleSeries(
                "minecraft:strength",
                List.of(
                        "provisional_meat_skewer.json",
                        "provisional_fortified_meat_soup.json",
                        "provisional_warrior_meat_pie.json",
                        "provisional_hero_roast.json",
                        "provisional_hero_table.json"
                ),
                List.of(
                        new RoleEffect("craftbound:adventurer_meal_armor", 1.0D),
                        new RoleEffect("craftbound:adventurer_meal_projectile_reduction", 0.05D),
                        new RoleEffect("craftbound:adventurer_meal_explosion_reduction", 0.05D),
                        new RoleEffect("craftbound:adventurer_meal_action_resistance", 0.10D),
                        new RoleEffect("craftbound:adventurer_meal_burning_resistance", 0.10D)
                )
        );
        assertRoleSeries(
                "minecraft:night_vision",
                List.of(
                        "provisional_berry_bread.json",
                        "provisional_vegetable_wrap.json",
                        "provisional_cave_travel_bread.json",
                        "provisional_cave_mushroom_stew.json",
                        "provisional_horizon_table.json"
                ),
                List.of(
                        new RoleEffect("craftbound:explorer_meal_endurance", 0.05D),
                        new RoleEffect("craftbound:explorer_meal_tool_care", 0.05D),
                        new RoleEffect("craftbound:explorer_meal_sure_footed", 0.125D),
                        new RoleEffect("craftbound:explorer_meal_climbing", 0.10D),
                        new RoleEffect("craftbound:explorer_meal_diving", 0.08D)
                )
        );
        assertRoleSeries(
                "minecraft:haste",
                List.of(
                        "provisional_builder_vegetable_bread.json",
                        "provisional_masons_egg_porridge.json",
                        "provisional_foreman_soup.json",
                        "provisional_master_builder_wrap.json",
                        "provisional_architect_table.json"
                ),
                List.of(
                        new RoleEffect("craftbound:architect_meal_demolition", 0.15D),
                        new RoleEffect("craftbound:architect_meal_fall_reduction", 0.10D),
                        new RoleEffect("craftbound:architect_meal_placement_reach", 0.25D),
                        new RoleEffect("craftbound:architect_meal_scaffolding", 0.10D),
                        new RoleEffect("craftbound:architect_meal_firework_conservation", 0.10D)
                )
        );
    }

    @Test
    void blacksmithAndAlchemistMealsHaveNoTemporaryPlaceholderBuffs() {
        for (String file : List.of(
                "provisional_work_stew.json",
                "provisional_iron_pot_meat_wrap.json",
                "provisional_hearth_meat_dish.json",
                "provisional_artisan_meat_pie.json",
                "provisional_master_table.json",
                "provisional_mushroom_soup.json",
                "provisional_sweet_berry_stew.json",
                "provisional_glow_berry_milk_porridge.json",
                "provisional_cocoa_tonic_pudding.json",
                "provisional_alchemist_table.json"
        )) {
            JsonObject recipe = resource(FOOD_RECIPE_BASE + file);
            assertFalse(recipe.has("effect"), file);
            assertFalse(recipe.has("effects"), file);
        }
    }

    private static void assertRecipePolicy(String file, int rank, String policy) {
        JsonObject recipe = resource(FOOD_RECIPE_BASE + file);
        assertEquals(rank, recipe.get("required_recipe_rank").getAsInt());
        assertEquals(policy, recipe.get("automation_policy").getAsString());
    }

    private static void assertRoleSeries(
            String vanillaEffect,
            List<String> files,
            List<RoleEffect> roleEffects
    ) {
        int[] durations = {300, 480, 720, 960, 1200};
        for (int rankIndex = 0; rankIndex < files.size(); rankIndex++) {
            String file = files.get(rankIndex);
            JsonObject recipe = resource(FOOD_RECIPE_BASE + file);
            var effects = recipe.getAsJsonArray("effects");
            assertEquals(rankIndex + 2, effects.size(), file);
            JsonObject vanilla = effects.get(0).getAsJsonObject();
            assertEquals("mob_effect", vanilla.get("type").getAsString());
            assertEquals(vanillaEffect, vanilla.get("id").getAsString());
            assertEquals(0, vanilla.get("amplifier").getAsInt());
            assertEquals(durations[rankIndex], vanilla.get("duration_seconds").getAsInt());

            for (int effectIndex = 0; effectIndex <= rankIndex; effectIndex++) {
                JsonObject attribute = effects.get(effectIndex + 1).getAsJsonObject();
                RoleEffect expected = roleEffects.get(effectIndex);
                assertEquals("attribute_modifier", attribute.get("type").getAsString());
                assertEquals(expected.id(), attribute.get("id").getAsString());
                assertEquals(expected.amount(), attribute.get("amount").getAsDouble(), 0.000001D);
                assertEquals(durations[rankIndex], attribute.get("duration_seconds").getAsInt());
            }
        }
    }

    private record RoleEffect(String id, double amount) {
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
