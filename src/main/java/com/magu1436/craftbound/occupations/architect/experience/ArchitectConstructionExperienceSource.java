package com.magu1436.craftbound.occupations.architect.experience;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.experience.source.ExperienceSource;
import net.puffish.skillsmod.api.experience.source.ExperienceSourceDisposeContext;
import net.puffish.skillsmod.api.util.Result;

import com.magu1436.craftbound.common.CraftboundUtilities;

/**
 * 建築家の施工経験値をPufferfish's Skillsへ渡す経験値源。
 */
public final class ArchitectConstructionExperienceSource
    implements ExperienceSource {

    public static final ResourceLocation ID =
        CraftboundUtilities.createResourceLocation(
            "architect_construction"
        );

    private ArchitectConstructionExperienceSource() {
    }

    public static void register() {
        SkillsAPI.registerExperienceSource(
            ID,
            context -> Result.success(
                new ArchitectConstructionExperienceSource()
            )
        );
    }

    public static void award(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return;
        }
        SkillsAPI.updateExperienceSources(
            player,
            ArchitectConstructionExperienceSource.class,
            source -> amount
        );
    }

    @Override
    public void dispose(ExperienceSourceDisposeContext context) {
    }
}
