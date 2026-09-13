package com.magu1436.craftbound.occupations.foodproducer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityCategory;

class FoodProducerDataDesignTest {

    private static final String SKILL_BASE =
            "/data/craftbound/puffish_skills/categories/food_producer/";
    private static final String FOOD_RECIPE_BASE =
            "/data/craftbound/craftbound_food_recipes/";
    private static final String REGIONAL_PLACED_FEATURE_BASE =
            "/data/craftbound/worldgen/placed_feature/";
    private static final String REGIONAL_CONFIGURED_FEATURE_BASE =
            "/data/craftbound/worldgen/configured_feature/";
    private static final List<String> REGIONAL_INGREDIENTS = List.of(
            "wild_garlic",
            "forest_thyme",
            "juniper_berry",
            "cactus_fig",
            "water_celery",
            "jungle_pepper",
            "cherry_herb",
            "alpine_leek",
            "mushroom_truffle",
            "ice_crystal_berry",
            "badlands_saffron"
    );
    private static final Map<Integer, List<String>> ROLE_MEALS_BY_RANK = Map.of(
            1, List.of("provisional_work_stew", "provisional_berry_bread",
                    "provisional_meat_skewer", "provisional_mushroom_soup",
                    "provisional_builder_vegetable_bread"),
            2, List.of("provisional_iron_pot_meat_wrap", "provisional_vegetable_wrap",
                    "provisional_fortified_meat_soup", "provisional_sweet_berry_stew",
                    "provisional_masons_egg_porridge"),
            3, List.of("provisional_hearth_meat_dish", "provisional_cave_travel_bread",
                    "provisional_warrior_meat_pie", "provisional_glow_berry_milk_porridge",
                    "provisional_foreman_soup"),
            4, List.of("provisional_artisan_meat_pie", "provisional_cave_mushroom_stew",
                    "provisional_hero_roast", "provisional_cocoa_tonic_pudding",
                    "provisional_master_builder_wrap"),
            5, List.of("provisional_master_table", "provisional_horizon_table",
                    "provisional_hero_table", "provisional_alchemist_table",
                    "provisional_architect_table")
    );

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
    void skillProgressionMatchesMultiplayerIntegrationBaseline() {
        JsonObject category = resource(SKILL_BASE + "category.json");
        JsonObject experience = resource(SKILL_BASE + "experience.json");
        JsonObject skills = resource(SKILL_BASE + "skills.json");
        JsonObject definitions = resource(SKILL_BASE + "definitions.json");

        assertTrue(category.get("unlocked_by_default").getAsBoolean());
        assertFalse(category.get("exclusive_root").getAsBoolean());
        assertEquals(50, experience.get("level_limit").getAsInt());
        assertEquals(
                "10 + level * 5",
                experience.getAsJsonObject("experience_per_level")
                        .getAsJsonObject("data")
                        .get("expression")
                        .getAsString()
        );
        assertEquals(0, experience.getAsJsonArray("sources").size());

        int cumulativeExperience = 0;
        for (int level = 0; level < 50; level++) {
            cumulativeExperience += 10 + level * 5;
        }
        assertEquals(6_625, cumulativeExperience);

        assertEquals(50, skills.size());
        assertEquals(50, definitions.size());
        assertTrue(definitions.entrySet().stream().allMatch(entry ->
                entry.getValue().getAsJsonObject().get("cost").getAsInt() == 1
        ));
        int totalSkillCost = definitions.entrySet().stream()
                .mapToInt(entry -> entry.getValue().getAsJsonObject().get("cost").getAsInt())
                .sum();
        assertEquals(50, totalSkillCost);

        assertEquals(19L, skills.keySet().stream()
                .filter(FoodProducerDataDesignTest::isAgricultureSkill)
                .count());
        assertEquals(17L, skills.keySet().stream()
                .filter(FoodProducerDataDesignTest::isRanchSkill)
                .count());
        assertEquals(14L, skills.keySet().stream()
                .filter(FoodProducerDataDesignTest::isProcessingSkill)
                .count());
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
    void preservedFoodsMatchDocumentedFoodValuesAndExperience() {
        assertFoodValues("provisional_dried_fruit_wrap", 6, 5.0D, 2);
        assertFoodValues("provisional_dried_mixed_pack", 8, 7.0D, 2);
        assertFoodValues("provisional_dried_meat_ration", 10, 9.0D, 2);
        assertFoodValues("provisional_deluxe_dried_mix", 12, 12.0D, 2);
        assertFoodValues("provisional_ultimate_ration", 14, 16.0D, 2);
    }

    @Test
    void roleMealsUseDocumentedFoodValuesByRank() {
        int[] nutritionByRank = {4, 5, 6, 7, 8};
        double[] saturationByRank = {3.0D, 4.0D, 5.0D, 6.0D, 7.0D};

        ROLE_MEALS_BY_RANK.forEach((rank, files) -> files.forEach(file ->
                assertFoodValues(
                        file,
                        nutritionByRank[rank - 1],
                        saturationByRank[rank - 1],
                        5
                )
        ));
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
        Set<String> knives = knifeTag.getAsJsonArray("values").asList().stream()
                .map(value -> value.getAsString())
                .collect(Collectors.toSet());
        assertTrue(knives.contains("craftbound:cooking_knife"));
        assertTrue(knives.contains("craftbound:blacksmith_cooking_knife"));
        resource("/assets/craftbound/models/item/blacksmith_cooking_knife.json");
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

    @Test
    void roleMealsRequireMatchingRegionalIngredientTier() {
        ROLE_MEALS_BY_RANK.forEach((rank, files) -> files.forEach(file -> {
            JsonObject recipe = resource(FOOD_RECIPE_BASE + file + ".json");
            assertEquals(rank, recipe.get("required_recipe_rank").getAsInt(), file);
            var inputs = recipe.getAsJsonArray("inputs");
            assertEquals(3, inputs.size(), file);
            long regionalInputs = inputs.asList().stream()
                    .map(value -> value.getAsJsonObject())
                    .filter(value -> value.has("tag"))
                    .filter(value -> value.get("tag").getAsString().equals(
                            "craftbound:regional_ingredients/tier_" + rank
                    ))
                    .count();
            assertEquals(1L, regionalInputs, file);
        }));

        assertEquals(24L * 60L * 60L * 20L,
                FoodQualityCategory.REGIONAL_INGREDIENT.stageDurationTicks());
    }

    @Test
    void qualityCategoriesUseFinalizedStageDurations() {
        assertEquals(70L * 60L * 20L,
                FoodQualityCategory.DISH.stageDurationTicks());
        assertEquals(140L * 60L * 20L,
                FoodQualityCategory.MATERIAL.stageDurationTicks());
        assertEquals(280L * 60L * 20L,
                FoodQualityCategory.PRESERVED_FOOD.stageDurationTicks());
        assertEquals(24L * 60L * 60L * 20L,
                FoodQualityCategory.REGIONAL_INGREDIENT.stageDurationTicks());

        assertEquals(140L * 60L * 20L,
                FoodQualityCategory.DISH.stageDurationTicks() * 2L);
        assertEquals(280L * 60L * 20L,
                FoodQualityCategory.MATERIAL.stageDurationTicks() * 2L);
        assertEquals(560L * 60L * 20L,
                FoodQualityCategory.PRESERVED_FOOD.stageDurationTicks() * 2L);
        assertEquals(48L * 60L * 60L * 20L,
                FoodQualityCategory.REGIONAL_INGREDIENT.stageDurationTicks() * 2L);
    }

    @Test
    void regionalForageUsesSharedIntegrationDensityAndPatchAttempts() {
        for (String ingredient : REGIONAL_INGREDIENTS) {
            JsonObject placed = resource(
                    REGIONAL_PLACED_FEATURE_BASE + ingredient + "_patch.json"
            );
            var placement = placed.getAsJsonArray("placement");
            JsonObject rarity = placement.get(0).getAsJsonObject();
            assertEquals("minecraft:rarity_filter", rarity.get("type").getAsString(), ingredient);
            assertEquals(16, rarity.get("chance").getAsInt(), ingredient);
            assertEquals("minecraft:in_square",
                    placement.get(1).getAsJsonObject().get("type").getAsString(), ingredient);
            assertEquals("MOTION_BLOCKING_NO_LEAVES",
                    placement.get(2).getAsJsonObject().get("heightmap").getAsString(), ingredient);
            assertEquals("minecraft:biome",
                    placement.get(3).getAsJsonObject().get("type").getAsString(), ingredient);

            JsonObject configured = resource(
                    REGIONAL_CONFIGURED_FEATURE_BASE + ingredient + "_patch.json"
            );
            assertEquals(6,
                    configured.getAsJsonObject("config").get("tries").getAsInt(), ingredient);
        }
    }

    private static void assertRecipePolicy(String file, int rank, String policy) {
        JsonObject recipe = resource(FOOD_RECIPE_BASE + file);
        assertEquals(rank, recipe.get("required_recipe_rank").getAsInt());
        assertEquals(policy, recipe.get("automation_policy").getAsString());
    }

    private static void assertFoodValues(
            String file,
            int nutrition,
            double saturationGain,
            int experience
    ) {
        JsonObject recipe = resource(FOOD_RECIPE_BASE + file + ".json");
        JsonObject food = recipe.getAsJsonObject("food");
        assertEquals(nutrition, food.get("nutrition").getAsInt(), file);
        assertEquals(
                saturationGain,
                food.get("saturation_gain").getAsDouble(),
                0.000001D,
                file
        );
        assertEquals(experience, recipe.get("experience").getAsInt(), file);
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

    private static boolean isAgricultureSkill(String skill) {
        return skill.equals("farmland_diagnosis")
                || skill.startsWith("growth_management_")
                || skill.startsWith("yield_management_")
                || skill.startsWith("quality_cultivation_")
                || skill.startsWith("fertility_management_");
    }

    private static boolean isRanchSkill(String skill) {
        return skill.equals("ranch_management")
                || skill.startsWith("feed_management_")
                || skill.startsWith("ranch_capacity_")
                || skill.startsWith("breeding_management_")
                || skill.startsWith("meat_processing_");
    }

    private static boolean isProcessingSkill(String skill) {
        return skill.equals("basic_processing")
                || skill.startsWith("processing_technique_")
                || skill.startsWith("quality_cooking_")
                || skill.startsWith("recipe_research_");
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
