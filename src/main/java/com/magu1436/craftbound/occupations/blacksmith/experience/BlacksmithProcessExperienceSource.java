package com.magu1436.craftbound.occupations.blacksmith.experience;

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

/** Pufferfish's Skillsへ鍛冶工程の経験値を渡す経験値源。 */
public final class BlacksmithProcessExperienceSource
    implements ExperienceSource {

    public static final ResourceLocation CATEGORY_ID =
        CraftboundUtilities.createResourceLocation("blacksmith");
    public static final ResourceLocation ID =
        CraftboundUtilities.createResourceLocation("blacksmith_process");

    private BlacksmithProcessExperienceSource() {
    }

    public static void register() {
        SkillsAPI.registerExperienceSource(
            ID,
            context -> Result.success(
                new BlacksmithProcessExperienceSource()
            )
        );
    }

    public static AwardResult award(ServerPlayer player, int amount) {
        Objects.requireNonNull(player, "player is null");
        if (amount <= 0) {
            return AwardResult.success(0);
        }

        try {
            Category category = SkillsAPI.getCategory(CATEGORY_ID).orElse(null);
            if (category == null) {
                return AwardResult.failure();
            }
            Experience experience = category.getExperience().orElse(null);
            if (experience == null) {
                return AwardResult.failure();
            }

            int level = experience.getLevel(player);
            if (experience.getRequired(player, level) <= 0) {
                return AwardResult.success(0);
            }

            int before = experience.getTotal(player);
            SkillsAPI.updateExperienceSources(
                player,
                BlacksmithProcessExperienceSource.class,
                source -> amount
            );
            int granted = Math.max(0, experience.getTotal(player) - before);
            return AwardResult.success(granted);
        } catch (RuntimeException exception) {
            return AwardResult.failure();
        }
    }

    @Override
    public void dispose(ExperienceSourceDisposeContext context) {
    }

    public record AwardResult(boolean successful, int grantedExperience) {
        private static AwardResult success(int grantedExperience) {
            return new AwardResult(true, grantedExperience);
        }

        private static AwardResult failure() {
            return new AwardResult(false, 0);
        }
    }
}
