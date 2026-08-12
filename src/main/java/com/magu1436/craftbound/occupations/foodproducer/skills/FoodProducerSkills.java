package com.magu1436.craftbound.occupations.foodproducer.skills;

import java.util.List;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.common.PuffishSkillsUtilities;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** FoodProducerのPufferfish's Skills識別子と取得状態の参照窓口. */
public final class FoodProducerSkills {

    public static final ResourceLocation CATEGORY_ID = ResourceLocation.fromNamespaceAndPath(
            Craftbound.MODID,
            "food_producer"
    );

    public static final String ROOT = "food_producer_root";
    public static final String FARMLAND_DIAGNOSIS = "farmland_diagnosis";
    public static final List<String> GROWTH_MANAGEMENT = ranks("growth_management", 5);
    public static final List<String> YIELD_MANAGEMENT = ranks("yield_management", 5);
    public static final List<String> QUALITY_CULTIVATION = ranks("quality_cultivation", 5);
    public static final List<String> FERTILITY_MANAGEMENT = ranks("fertility_management", 3);

    public static final String RANCH_MANAGEMENT = "ranch_management";
    public static final List<String> FEED_MANAGEMENT = ranks("feed_management", 3);
    public static final List<String> RANCH_CAPACITY = ranks("ranch_capacity", 3);
    public static final List<String> BREEDING_MANAGEMENT = ranks("breeding_management", 5);
    public static final List<String> MEAT_PROCESSING = ranks("meat_processing", 5);

    public static final String BASIC_PROCESSING = "basic_processing";
    public static final List<String> PROCESSING_TECHNIQUE = ranks("processing_technique", 3);
    public static final List<String> QUALITY_COOKING = ranks("quality_cooking", 5);
    public static final List<String> RECIPE_RESEARCH = ranks("recipe_research", 5);

    private FoodProducerSkills() {
    }

    public static boolean has(ServerPlayer player, String skillId) {
        return PuffishSkillsUtilities.hasSkill(player, CATEGORY_ID, skillId);
    }

    public static int growthManagementRank(ServerPlayer player) {
        return rank(player, GROWTH_MANAGEMENT);
    }

    public static int yieldManagementRank(ServerPlayer player) {
        return rank(player, YIELD_MANAGEMENT);
    }

    public static int qualityCultivationRank(ServerPlayer player) {
        return rank(player, QUALITY_CULTIVATION);
    }

    public static int fertilityManagementRank(ServerPlayer player) {
        return rank(player, FERTILITY_MANAGEMENT);
    }

    public static int recipeResearchRank(ServerPlayer player) {
        return rank(player, RECIPE_RESEARCH);
    }

    public static int rank(ServerPlayer player, List<String> skillIds) {
        return PuffishSkillsUtilities.getHighestUnlockedRank(player, CATEGORY_ID, skillIds);
    }

    private static List<String> ranks(String baseId, int maximumRank) {
        return java.util.stream.IntStream.rangeClosed(1, maximumRank)
                .mapToObj(rank -> baseId + "_" + rank)
                .toList();
    }
}
