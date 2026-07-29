package com.magu1436.craftbound.occupations.adventurer.experience;

import java.util.Set;
import java.util.UUID;

import com.magu1436.craftbound.common.CraftboundUtilities;
import com.magu1436.craftbound.occupations.adventurer.AdventurerConfig;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.Experience;
import net.puffish.skillsmod.api.SkillsAPI;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

/**
 * Mob死亡時に記録済みの参加者へ経験値を配布する。
 */
public final class MobExperienceAwardService {
    private static final ResourceLocation ADVENTURER_CATEGORY_ID =
        CraftboundUtilities.createResourceLocation("adventurer");

    private MobExperienceAwardService() {
    }

    public static void awardParticipants(
        LivingDeathEvent event
    ) {
        LivingEntity entity = event.getEntity();

        if (entity.level().isClientSide()) {
            return;
        }

        try {
            MobExperienceDefinition definition =
                MobExperienceRegistry
                    .find(entity.getType())
                    .orElse(null);

            if (
                definition == null
                    || definition.experience() <= 0
            ) {
                return;
            }

            Category category = SkillsAPI
                .getCategory(ADVENTURER_CATEGORY_ID)
                .orElse(null);

            if (category == null) {
                return;
            }

            Experience experience =
                category.getExperience().orElse(null);

            if (experience == null) {
                return;
            }

            ServerLevel serverLevel = (ServerLevel) entity.level();
            MinecraftServer server = serverLevel.getServer();
            Set<UUID> participantIds =
                MobExperienceParticipantTracker.getParticipants(
                    entity
                );

            for (UUID participantId : participantIds) {
                ServerPlayer player = server
                    .getPlayerList()
                    .getPlayer(participantId);

                if (
                    player == null
                        || !category.isUnlocked(player)
                        || !canReceiveExperience(
                            definition,
                            experience,
                            player
                        )
                ) {
                    continue;
                }

                AdventurerMobKillExperienceSource.award(
                    player,
                    definition.experience()
                );
            }
        } finally {
            MobExperienceParticipantTracker.clear(entity);
        }
    }

    private static boolean canReceiveExperience(
        MobExperienceDefinition definition,
        Experience experience,
        ServerPlayer player
    ) {
        if (!definition.category().isNormalMob()) {
            return true;
        }

        if (!AdventurerConfig.isNormalMobExperienceEnabled()) {
            return false;
        }

        return experience.getLevel(player)
            < AdventurerConfig.getNormalMobExperienceLevelLimit();
    }
}
