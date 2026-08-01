package com.magu1436.craftbound.event;

import java.util.Optional;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.architect.client.ClientPlayerPlacedBlockCache;
import com.magu1436.craftbound.occupations.architect.demolition.DemolitionConditions;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 同期キャッシュを使ってクライアントの破壊進捗表示を補正する。
 */
@Mod.EventBusSubscriber(
    modid = Craftbound.MODID,
    value = Dist.CLIENT,
    bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class ClientDemolitionBreakSpeedEventHandler {

    private ClientDemolitionBreakSpeedEventHandler() {
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!event.getEntity().level().isClientSide()) {
            return;
        }

        Optional<BlockPos> position = event.getPosition();
        if (position.isEmpty()) {
            return;
        }

        BlockPos pos = position.get();
        if (!ClientPlayerPlacedBlockCache.contains(pos)
            || !DemolitionConditions.canApply(
                event,
                pos,
                isSupportedGameMode()
            )) {
            return;
        }

        DemolitionConditions.apply(event);
    }

    private static boolean isSupportedGameMode() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode == null) {
            return false;
        }

        GameType gameMode = minecraft.gameMode.getPlayerMode();
        return gameMode == GameType.SURVIVAL
            || gameMode == GameType.ADVENTURE;
    }
}
