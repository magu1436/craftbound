package com.magu1436.craftbound.event;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.explorer.DivingAirConsumptionService;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingBreatheEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 潜水術による空気消費軽減を適用する。
 */
@Mod.EventBusSubscriber(
    modid = Craftbound.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class CraftboundDivingEventHandler {

    private CraftboundDivingEventHandler() {
    }

    @SubscribeEvent
    public static void onLivingBreathe(LivingBreatheEvent event) {
        if (
            event.getEntity() instanceof ServerPlayer player
                && !(player instanceof FakePlayer)
        ) {
            DivingAirConsumptionService.preventAirConsumption(
                player,
                event
            );
        }
    }
}
