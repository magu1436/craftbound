package com.magu1436.craftbound.occupations.explorer.integration;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** 探索経験値を外部スキルModへ渡す境界。 */
public interface ExplorerExperienceGateway {
    boolean isAtMaximumLevel(ServerPlayer player);

    ExperienceGrantResult grantExperience(
        ServerPlayer player,
        ResourceLocation sourceId,
        int amount
    );

    enum ExperienceGrantResult {
        SUCCESS,
        INVALID_AMOUNT,
        SOURCE_UNAVAILABLE,
        CATEGORY_UNAVAILABLE,
        EXPERIENCE_UNAVAILABLE,
        GRANT_FAILED
    }
}
