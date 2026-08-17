package com.magu1436.craftbound.occupations.blacksmith.event;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.network.CraftboundNetwork;
import com.magu1436.craftbound.network.packet.BlacksmithQualityPerformanceSyncPacket;
import com.magu1436.craftbound.occupations.blacksmith.data.BlacksmithQualityPerformanceDefinitions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
    modid = Craftbound.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class BlacksmithQualityPerformanceSyncEvents {
    private BlacksmithQualityPerformanceSyncEvents() {}

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        BlacksmithQualityPerformanceSyncPacket packet = new BlacksmithQualityPerformanceSyncPacket(
            BlacksmithQualityPerformanceDefinitions.current()
        );
        for (ServerPlayer player : event.getPlayers()) {
            CraftboundNetwork.sendToPlayer(player, packet);
        }
    }
}
