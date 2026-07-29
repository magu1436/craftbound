package com.magu1436.craftbound.event;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.adventurer.events.AdventurerBurningResistanceService;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
    modid = Craftbound.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class CraftboundBurningResistanceEventHandler {

    private CraftboundBurningResistanceEventHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (
            event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
        ) {
            return;
        }

        AdventurerBurningResistanceService.reduceUpdatedDuration(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(
        PlayerEvent.PlayerLoggedInEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AdventurerBurningResistanceService.initialize(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(
        PlayerEvent.PlayerLoggedOutEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AdventurerBurningResistanceService.remove(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer originalPlayer) {
            AdventurerBurningResistanceService.remove(originalPlayer);
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            AdventurerBurningResistanceService.initialize(player);
        }
    }
}
