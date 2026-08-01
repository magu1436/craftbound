package com.magu1436.craftbound.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.architect.ConstructionTrackingService;
import com.magu1436.craftbound.occupations.architect.events.PlayerPlacedBlockRemovalQueue;
import com.magu1436.craftbound.occupations.architect.experience.ArchitectExperienceService;
import com.magu1436.craftbound.occupations.architect.experience.ConstructionScheduler;
import com.magu1436.craftbound.occupations.architect.capability.ConstructionChunkDataAccess;
import com.magu1436.craftbound.occupations.architect.network.PlayerPlacedBlockSync;

/**
 * プレイヤー設置ブロックの記録、削除追跡、同期契機を処理する。
 */
@Mod.EventBusSubscriber(
    modid = Craftbound.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class CraftboundPlayerPlacedBlockEventHandler {

    private CraftboundPlayerPlacedBlockEventHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
            || !(event.getEntity() instanceof ServerPlayer player)
            || player instanceof FakePlayer) {
            return;
        }

        if (event instanceof BlockEvent.EntityMultiPlaceEvent multiEvent) {
            for (BlockSnapshot snapshot
                : multiEvent.getReplacedBlockSnapshots()) {
                handlePlacedPosition(
                    player,
                    level,
                    snapshot.getPos(),
                    level.getBlockState(snapshot.getPos())
                );
            }
        } else {
            handlePlacedPosition(
                player,
                level,
                event.getPos(),
                event.getPlacedBlock()
            );
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            PlayerPlacedBlockRemovalQueue.enqueueWithAdjacentBlocks(
                level,
                event.getPos()
            );
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onExplosionDetonate(
        ExplosionEvent.Detonate event
    ) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        for (BlockPos pos : event.getAffectedBlocks()) {
            PlayerPlacedBlockRemovalQueue.enqueueWithAdjacentBlocks(
                level,
                pos
            );
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END
            && event.level instanceof ServerLevel level) {
            PlayerPlacedBlockRemovalQueue.flush(level);
            ConstructionScheduler.tick(level);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            PlayerPlacedBlockRemovalQueue.clear(level);
            ConstructionScheduler.clear(level);
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
            || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        ConstructionChunkDataAccess.getPendingConstructions(chunk)
            .forEach(entry -> ConstructionScheduler.schedule(
                level,
                entry.pos(),
                entry.pending()
            ));
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(
        PlayerEvent.PlayerLoggedInEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ArchitectExperienceService.deliverPending(player);
        }
    }

    @SubscribeEvent
    public static void onChunkWatch(ChunkWatchEvent.Watch event) {
        PlayerPlacedBlockSync.sendSnapshot(
            event.getPlayer(),
            event.getLevel(),
            event.getChunk()
        );
    }

    @SubscribeEvent
    public static void onChunkUnwatch(ChunkWatchEvent.UnWatch event) {
        PlayerPlacedBlockSync.sendForget(
            event.getPlayer(),
            event.getLevel(),
            event.getPos()
        );
    }

    private static void handlePlacedPosition(
        ServerPlayer player,
        ServerLevel level,
        BlockPos pos,
        BlockState state
    ) {
        ConstructionTrackingService.onPlaced(player, level, pos, state);
    }
}
