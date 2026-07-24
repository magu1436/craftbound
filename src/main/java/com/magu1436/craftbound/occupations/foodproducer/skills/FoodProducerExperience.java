package com.magu1436.craftbound.occupations.foodproducer.skills;

import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.api.SkillsAPI;

/** FoodProducerカテゴリの経験値をサーバー側で加算する. */
public final class FoodProducerExperience {

    private FoodProducerExperience() {
    }

    public static boolean add(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return false;
        }

        return SkillsAPI.getCategory(FoodProducerSkills.CATEGORY_ID)
                .flatMap(category -> category.getExperience())
                .map(experience -> {
                    experience.addTotal(player, amount);
                    return true;
                })
                .orElse(false);
    }
}
