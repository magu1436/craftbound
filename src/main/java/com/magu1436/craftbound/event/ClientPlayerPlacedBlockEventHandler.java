package com.magu1436.craftbound.event;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.architect.client.ClientPlayerPlacedBlockCache;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * クライアントの設置履歴キャッシュをライフサイクルに合わせて破棄する。
 */
@Mod.EventBusSubscriber(
    modid = Craftbound.MODID,
    value = Dist.CLIENT,
    bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class ClientPlayerPlacedBlockEventHandler {

    private ClientPlayerPlacedBlockEventHandler() {
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            ClientPlayerPlacedBlockCache.removeChunk(
                event.getChunk().getPos()
            );
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            ClientPlayerPlacedBlockCache.clear();
        }
    }

    @SubscribeEvent
    public static void onLogout(
        ClientPlayerNetworkEvent.LoggingOut event
    ) {
        ClientPlayerPlacedBlockCache.clear();
    }
}
