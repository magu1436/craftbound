package com.magu1436.craftbound.event;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.adventurer.events.AdventurerDamageEventService;

import net.minecraftforge.event.entity.living.LivingHurtEvent;
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
}