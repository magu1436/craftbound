package com.magu1436.craftbound.occupations.adventurer.client;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.network.CraftboundNetwork;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 緊急回避キーの入力をC2Sパケットへ変換する。
 */
@Mod.EventBusSubscriber(
    modid = Craftbound.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE,
    value = Dist.CLIENT
)
public final class EmergencyEvasionClientInputHandler {

    private EmergencyEvasionClientInputHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        while (
            EmergencyEvasionKeyMappings.EMERGENCY_EVASION
                .get()
                .consumeClick()
        ) {
            if (minecraft.player != null) {
                CraftboundNetwork.sendEmergencyEvasionRequest();
            }
        }
    }
}
