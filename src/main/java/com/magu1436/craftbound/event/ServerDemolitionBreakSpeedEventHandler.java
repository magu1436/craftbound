package com.magu1436.craftbound.event;

import java.util.Optional;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.architect.capability.PlayerPlacedBlockAccess;
import com.magu1436.craftbound.occupations.architect.demolition.DemolitionConditions;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * サーバーのCapabilityを正本として実際の破壊速度を補正する。
 */
@Mod.EventBusSubscriber(
    modid = Craftbound.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class ServerDemolitionBreakSpeedEventHandler {

    private ServerDemolitionBreakSpeedEventHandler() {
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
            || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        Optional<BlockPos> position = event.getPosition();
        if (position.isEmpty()) {
            return;
        }

        BlockPos pos = position.get();
        if (!PlayerPlacedBlockAccess.contains(level, pos)
            || !DemolitionConditions.canApply(
                event,
                pos,
                isSupportedGameMode(player)
            )) {
            return;
        }

        DemolitionConditions.apply(event);
    }

    private static boolean isSupportedGameMode(ServerPlayer player) {
        GameType gameMode = player.gameMode.getGameModeForPlayer();
        return gameMode == GameType.SURVIVAL
            || gameMode == GameType.ADVENTURE;
    }
}
