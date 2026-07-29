package com.magu1436.craftbound.occupations.adventurer.experience;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;

/**
 * 破棄されるMobの討伐参加者記録を消去する。
 */
public final class MobExperienceParticipantCleanupService {
    private MobExperienceParticipantCleanupService() {
    }

    public static void clearIfDestroyed(
        EntityLeaveLevelEvent event
    ) {
        Entity entity = event.getEntity();

        if (
            entity.level().isClientSide()
                || !(entity instanceof LivingEntity livingEntity)
                || entity instanceof Player
        ) {
            return;
        }

        Entity.RemovalReason removalReason =
            entity.getRemovalReason();

        if (
            removalReason != null
                && removalReason.shouldDestroy()
        ) {
            MobExperienceParticipantTracker.clear(livingEntity);
        }
    }
}
