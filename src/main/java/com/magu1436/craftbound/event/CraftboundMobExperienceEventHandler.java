package com.magu1436.craftbound.event;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.adventurer.experience.MobExperienceAwardService;
import com.magu1436.craftbound.occupations.adventurer.experience.MobExperienceParticipantCleanupService;
import com.magu1436.craftbound.occupations.adventurer.experience.MobExperienceParticipationService;

import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 冒険家のMob経験値に関するForgeイベントを受け取る。
 */
@Mod.EventBusSubscriber(
    modid = Craftbound.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class CraftboundMobExperienceEventHandler {
    private CraftboundMobExperienceEventHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        MobExperienceParticipationService.recordParticipant(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        MobExperienceAwardService.awardParticipants(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityLeaveLevel(
        EntityLeaveLevelEvent event
    ) {
        MobExperienceParticipantCleanupService.clearIfDestroyed(
            event
        );
    }
}
