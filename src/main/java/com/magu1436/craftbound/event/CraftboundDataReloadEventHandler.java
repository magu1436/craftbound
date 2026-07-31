package com.magu1436.craftbound.event;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.adventurer.data.DeathlineClearEffectDefinitions;
import com.magu1436.craftbound.occupations.adventurer.data.DeathlineExcludedDamageDefinitions;
import com.magu1436.craftbound.occupations.explorer.data.ToolCareBlockDefinitions;

import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * サーバーデータのリロードリスナーを登録する。
 */
@Mod.EventBusSubscriber(
    modid = Craftbound.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class CraftboundDataReloadEventHandler {
    private CraftboundDataReloadEventHandler() {
    }

    @SubscribeEvent
    public static void addReloadListeners(
        AddReloadListenerEvent event
    ) {
        event.addListener(DeathlineClearEffectDefinitions.INSTANCE);
        event.addListener(
            DeathlineExcludedDamageDefinitions.INSTANCE
        );
        event.addListener(ToolCareBlockDefinitions.INSTANCE);
    }
}
