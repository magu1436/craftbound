package com.magu1436.craftbound.occupations.explorer.event;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.explorer.command.ExplorerCommand;
import com.magu1436.craftbound.occupations.explorer.discovery.ExplorerDiscoveryManager;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 探検家発見処理のForgeライフサイクル接続点。 */
@Mod.EventBusSubscriber(
    modid = Craftbound.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class ExplorerDiscoveryEvents {
    private static final ExplorerDiscoveryManager MANAGER =
        ExplorerDiscoveryManager.instance();

    private ExplorerDiscoveryEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ExplorerCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END
            && event.player instanceof ServerPlayer player) {
            MANAGER.tick(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clear(event);
    }

    @SubscribeEvent
    public static void onChangedDimension(
        PlayerEvent.PlayerChangedDimensionEvent event
    ) {
        clear(event);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        clear(event);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MANAGER.clear(player);
        }
    }

    @SubscribeEvent
    public static void onGameModeChanged(
        PlayerEvent.PlayerChangeGameModeEvent event
    ) {
        if (event.getNewGameMode() != GameType.SURVIVAL
            && event.getEntity() instanceof ServerPlayer player) {
            MANAGER.clear(player);
        }
    }

    private static void clear(PlayerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MANAGER.clear(player);
        }
    }
}
