package com.magu1436.craftbound.event;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.adventurer.events.AdventurerResistanceEventService;

import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Craftbound.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class CraftboundMobEffectEventHandler {

    private CraftboundMobEffectEventHandler() {
    }

    @SubscribeEvent
    public static void onMobEffectAdded(MobEffectEvent.Added event) {
        AdventurerResistanceEventService.reduceDuration(event);
    }
}
