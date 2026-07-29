package com.magu1436.craftbound.occupations.adventurer.experience;

import com.magu1436.craftbound.common.CraftboundUtilities;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.experience.source.ExperienceSource;
import net.puffish.skillsmod.api.experience.source.ExperienceSourceDisposeContext;
import net.puffish.skillsmod.api.util.Result;

/**
 * 冒険家のMob討伐経験値をPufferfish's Skillsへ渡す経験値源。
 */
public final class AdventurerMobKillExperienceSource
    implements ExperienceSource {

    public static final ResourceLocation ID =
        CraftboundUtilities.createResourceLocation("adventurer_mob_kill");

    private AdventurerMobKillExperienceSource() {
    }

    /**
     * 経験値源をPufferfish's Skillsへ登録する。
     */
    public static void register() {
        SkillsAPI.registerExperienceSource(
            ID,
            context -> Result.success(
                new AdventurerMobKillExperienceSource()
            )
        );
    }

    /**
     * 冒険家のMob討伐経験値源が設定されたカテゴリへ経験値を付与する。
     *
     * @param player 経験値を受け取るプレイヤー
     * @param amount 付与する経験値
     */
    public static void award(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return;
        }

        SkillsAPI.updateExperienceSources(
            player,
            AdventurerMobKillExperienceSource.class,
            source -> amount
        );
    }

    @Override
    public void dispose(ExperienceSourceDisposeContext context) {
    }
}
