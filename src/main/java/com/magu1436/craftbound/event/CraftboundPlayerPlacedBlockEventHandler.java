package com.magu1436.craftbound.event;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.architect.capability.PlayerPlacedBlockAccess;
import com.magu1436.craftbound.occupations.architect.events.PlayerPlacedBlockRemovalQueue;
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

        LongArrayList positions = new LongArrayList();
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multiEvent) {
            for (BlockSnapshot snapshot
                : multiEvent.getReplacedBlockSnapshots()) {
                positions.add(snapshot.getPos().asLong());
            }
        } else {
            positions.add(event.getPos().asLong());
        }
        addAndSync(level, positions);
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
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            PlayerPlacedBlockRemovalQueue.clear(level);
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

    private static void addAndSync(
        ServerLevel level,
        LongArrayList positions
    ) {
        Long2ObjectOpenHashMap<LongArrayList> positionsByChunk =
            new Long2ObjectOpenHashMap<>();
        for (long packedPos : positions) {
            BlockPos pos = BlockPos.of(packedPos);
            positionsByChunk
                .computeIfAbsent(
                    ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4),
                    ignored -> new LongArrayList()
                )
                .add(packedPos);
        }

        for (var entry : positionsByChunk.long2ObjectEntrySet()) {
            ChunkPos chunkPos = new ChunkPos(entry.getLongKey());
            LevelChunk chunk = level.getChunkSource().getChunkNow(
                chunkPos.x,
                chunkPos.z
            );
            if (chunk == null) {
                continue;
            }

            LongArrayList actuallyAdded = PlayerPlacedBlockAccess.addAll(
                chunk,
                entry.getValue()
            );
            if (!actuallyAdded.isEmpty()) {
                PlayerPlacedBlockSync.sendAdded(
                    level,
                    chunk,
                    actuallyAdded
                );
            }
        }
    }
}
