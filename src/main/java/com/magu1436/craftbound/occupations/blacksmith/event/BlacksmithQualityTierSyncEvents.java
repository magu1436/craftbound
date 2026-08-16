package com.magu1436.craftbound.occupations.blacksmith.event;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.network.CraftboundNetwork;
import com.magu1436.craftbound.network.packet.BlacksmithQualityTierSyncPacket;
import com.magu1436.craftbound.occupations.blacksmith.data.BlacksmithQualityTierDefinitions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
    modid = Craftbound.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class BlacksmithQualityTierSyncEvents {
    private BlacksmithQualityTierSyncEvents() {}

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        BlacksmithQualityTierSyncPacket packet = new BlacksmithQualityTierSyncPacket(
            BlacksmithQualityTierDefinitions.all()
        );
        for (ServerPlayer player : event.getPlayers()) {
            CraftboundNetwork.sendToPlayer(player, packet);
        }
    }
}
