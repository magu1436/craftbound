package com.magu1436.craftbound.event;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.adventurer.events.AdventurerDamageEventService;
import com.magu1436.craftbound.occupations.adventurer.events.DeathlineCrossingService;
import com.magu1436.craftbound.occupations.architect.events.ArchitectFallProtectionService;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Craftbound.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class CraftboundDamageEventHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        AdventurerDamageEventService.reductionDamage(event);
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        ArchitectFallProtectionService.reduceFallDamage(event);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (
            event.getEntity() instanceof ServerPlayer player
                && DeathlineCrossingService.tryActivate(
                    player,
                    event.getSource()
                )
        ) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (
            event.getEntity() instanceof ServerPlayer player
                && DeathlineCrossingService.shouldPreventDamage(
                    player,
                    event.getSource()
                )
        ) {
            event.setCanceled(true);
        }
    }
}
