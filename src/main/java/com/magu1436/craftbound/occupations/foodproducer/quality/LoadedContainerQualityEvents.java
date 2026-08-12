package com.magu1436.craftbound.occupations.foodproducer.quality;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.foodproducer.storage.PreservationMultiplierContainer;
import com.magu1436.craftbound.occupations.foodproducer.storage.PreservationStorageItemData;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 読み込み済みコンテナと、アイテム化した保存設備の内部品質時計を進める。 */
@Mod.EventBusSubscriber(modid = Craftbound.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LoadedContainerQualityEvents {

    private static final long UPDATE_INTERVAL_TICKS = 200L;
    private static final int MAX_CONTAINERS_PER_TICK = 32;
    private static final double NORMAL_CONTAINER_MULTIPLIER = 1.0D;
    private static final Map<ServerLevel, LevelSchedule> SCHEDULES = new IdentityHashMap<>();

    private LoadedContainerQualityEvents() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }

        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (blockEntity instanceof Container) {
                // Do not access container contents while the chunk is becoming FULL.
                // Randomizable containers may generate their loot and call setChanged(),
                // which would wait for this same chunk and deadlock world generation.
                schedule(
                        level,
                        blockEntity.getBlockPos(),
                        level.getGameTime() + UPDATE_INTERVAL_TICKS
                );
            }
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }

        long gameTime = level.getGameTime();
        LevelSchedule schedule = SCHEDULES.get(level);
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (blockEntity instanceof Container container) {
                advanceAndPauseContainer(container, blockEntity, gameTime);
                if (schedule != null) {
                    schedule.remove(blockEntity.getBlockPos());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getState().hasBlockEntity()) {
            schedule(level, event.getPos(), level.getGameTime() + UPDATE_INTERVAL_TICKS);
        }
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(event.getPos());
        if (blockEntity instanceof Container container) {
            advanceAndPauseContainer(container, blockEntity, level.getGameTime());
        }
        LevelSchedule schedule = SCHEDULES.get(level);
        if (schedule != null) {
            schedule.remove(event.getPos());
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }

        LevelSchedule schedule = SCHEDULES.get(level);
        if (schedule == null) {
            return;
        }

        long gameTime = level.getGameTime();
        for (int processed = 0; processed < MAX_CONTAINERS_PER_TICK; processed++) {
            ScheduledContainer scheduled = schedule.pollDue(gameTime);
            if (scheduled == null) {
                break;
            }

            BlockPos pos = BlockPos.of(scheduled.pos());
            if (!level.hasChunkAt(pos)) {
                continue;
            }

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof Container container) {
                advanceContainer(container, blockEntity, gameTime);
                schedule.add(pos, gameTime + UPDATE_INTERVAL_TICKS);
            }
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        LevelSchedule schedule = SCHEDULES.remove(level);
        if (schedule == null) {
            return;
        }

        long gameTime = level.getGameTime();
        for (long posValue : schedule.positions()) {
            BlockPos pos = BlockPos.of(posValue);
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof Container container) {
                advanceAndPauseContainer(container, blockEntity, gameTime);
            }
        }
    }

    private static void schedule(ServerLevel level, BlockPos pos, long dueTime) {
        SCHEDULES.computeIfAbsent(level, ignored -> new LevelSchedule()).add(pos, dueTime);
    }

    private static void advanceContainer(Container container, BlockEntity blockEntity, long gameTime) {
        boolean changed = false;
        double multiplier = preservationMultiplier(blockEntity);
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (FoodQualityItems.isQualityTarget(stack)) {
                changed |= FoodQualityData.advanceLoadedTime(
                        stack,
                        gameTime,
                        multiplier
                );
            }
            changed |= PreservationStorageItemData.advanceLoadedTime(stack, gameTime);
        }
        if (changed) {
            blockEntity.setChanged();
        }
    }

    private static void advanceAndPauseContainer(
            Container container,
            BlockEntity blockEntity,
            long gameTime
    ) {
        boolean changed = false;
        double multiplier = preservationMultiplier(blockEntity);
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (FoodQualityItems.isQualityTarget(stack)) {
                changed |= FoodQualityData.pauseClock(
                        stack,
                        gameTime,
                        multiplier
                );
            }
            changed |= PreservationStorageItemData.pauseClock(stack, gameTime);
        }
        if (changed) {
            blockEntity.setChanged();
        }
    }

    private static double preservationMultiplier(BlockEntity blockEntity) {
        return blockEntity instanceof PreservationMultiplierContainer preserving
                ? preserving.preservationMultiplier()
                : NORMAL_CONTAINER_MULTIPLIER;
    }

    private record ScheduledContainer(long pos, long dueTime) {
    }

    private static final class LevelSchedule {

        private final PriorityQueue<ScheduledContainer> queue = new PriorityQueue<>(
                Comparator.comparingLong(ScheduledContainer::dueTime));
        private final Map<Long, ScheduledContainer> current = new HashMap<>();

        void add(BlockPos pos, long dueTime) {
            long posValue = pos.asLong();
            if (current.containsKey(posValue)) {
                return;
            }
            ScheduledContainer scheduled = new ScheduledContainer(posValue, dueTime);
            current.put(posValue, scheduled);
            queue.add(scheduled);
        }

        void remove(BlockPos pos) {
            current.remove(pos.asLong());
        }

        ScheduledContainer pollDue(long gameTime) {
            while (!queue.isEmpty()) {
                ScheduledContainer scheduled = queue.peek();
                if (current.get(scheduled.pos()) != scheduled) {
                    queue.remove();
                    continue;
                }
                if (scheduled.dueTime() > gameTime) {
                    return null;
                }
                queue.remove();
                current.remove(scheduled.pos());
                return scheduled;
            }
            return null;
        }

        List<Long> positions() {
            return new ArrayList<>(current.keySet());
        }
    }
}
