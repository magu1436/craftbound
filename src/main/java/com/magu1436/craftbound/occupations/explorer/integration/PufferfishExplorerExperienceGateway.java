package com.magu1436.craftbound.occupations.explorer.integration;

import java.util.Objects;

import com.magu1436.craftbound.common.CraftboundUtilities;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.Experience;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.experience.source.ExperienceSource;
import net.puffish.skillsmod.api.experience.source.ExperienceSourceDisposeContext;
import net.puffish.skillsmod.api.util.Result;

/** Pufferfish's Skills APIを利用する探索経験値Gateway。 */
public final class PufferfishExplorerExperienceGateway
    implements ExplorerExperienceGateway {

    public static final ResourceLocation CATEGORY_ID =
        CraftboundUtilities.createResourceLocation("explorer");
    public static final ResourceLocation EXPERIENCE_SOURCE_ID =
        CraftboundUtilities.createResourceLocation("exploration_discovery");

    /** 探索経験値源をPufferfish's Skillsへ登録する。 */
    public static void register() {
        SkillsAPI.registerExperienceSource(
            EXPERIENCE_SOURCE_ID,
            context -> Result.success(new ExplorationDiscoverySource())
        );
    }

    @Override
    public boolean isAtMaximumLevel(ServerPlayer player) {
        Objects.requireNonNull(player, "player is null");
        Experience experience = findExperience();
        if (experience == null) {
            return false;
        }
        int level = experience.getLevel(player);
        return experience.getRequired(player, level) <= 0;
    }

    @Override
    public ExperienceGrantResult grantExperience(
        ServerPlayer player,
        ResourceLocation sourceId,
        int amount
    ) {
        Objects.requireNonNull(player, "player is null");
        Objects.requireNonNull(sourceId, "source id is null");
        if (amount < 0) {
            return ExperienceGrantResult.INVALID_AMOUNT;
        }
        if (!EXPERIENCE_SOURCE_ID.equals(sourceId)) {
            return ExperienceGrantResult.SOURCE_UNAVAILABLE;
        }
        if (amount == 0) {
            return ExperienceGrantResult.SUCCESS;
        }

        Category category = SkillsAPI.getCategory(CATEGORY_ID).orElse(null);
        if (category == null) {
            return ExperienceGrantResult.CATEGORY_UNAVAILABLE;
        }
        Experience experience = category.getExperience().orElse(null);
        if (experience == null) {
            return ExperienceGrantResult.EXPERIENCE_UNAVAILABLE;
        }
        int before = experience.getTotal(player);
        SkillsAPI.updateExperienceSources(
            player,
            ExplorationDiscoverySource.class,
            ignored -> amount
        );
        return experience.getTotal(player) > before
            ? ExperienceGrantResult.SUCCESS
            : ExperienceGrantResult.GRANT_FAILED;
    }

    private static Experience findExperience() {
        Category category = SkillsAPI.getCategory(CATEGORY_ID).orElse(null);
        return category == null
            ? null
            : category.getExperience().orElse(null);
    }

    private static final class ExplorationDiscoverySource
        implements ExperienceSource {

        @Override
        public void dispose(ExperienceSourceDisposeContext context) {
        }
    }
}
