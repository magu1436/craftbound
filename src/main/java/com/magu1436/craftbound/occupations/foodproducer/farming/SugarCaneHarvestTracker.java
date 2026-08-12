package com.magu1436.craftbound.occupations.foodproducer.farming;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;

/** サトウキビ1本の連鎖破壊へ、最初に破壊したプレイヤーを引き継ぐ. */
public final class SugarCaneHarvestTracker {

    private static final long CONTEXT_LIFETIME_TICKS = 5L;
    private static final Map<ServerLevel, Long2ObjectOpenHashMap<HarvestContext>> CONTEXTS =
            new IdentityHashMap<>();

    private SugarCaneHarvestTracker() {
    }

    public static void begin(ServerLevel level, BlockPos brokenPosition, ServerPlayer player) {
        int minimumY = brokenPosition.getY();
        while (level.getBlockState(new BlockPos(brokenPosition.getX(), minimumY - 1, brokenPosition.getZ()))
                .is(Blocks.SUGAR_CANE)) {
            minimumY--;
        }

        int maximumY = brokenPosition.getY();
        while (level.getBlockState(new BlockPos(brokenPosition.getX(), maximumY + 1, brokenPosition.getZ()))
                .is(Blocks.SUGAR_CANE)) {
            maximumY++;
        }

        contextMap(level).put(
                columnKey(brokenPosition),
                new HarvestContext(player, minimumY, maximumY, level.getGameTime() + CONTEXT_LIFETIME_TICKS)
        );
    }

    public static Optional<HarvestContext> find(ServerLevel level, BlockPos position) {
        HarvestContext context = contextMap(level).get(columnKey(position));
        if (context == null
                || context.expiresAtGameTime < level.getGameTime()
                || position.getY() < context.minimumY
                || position.getY() > context.maximumY) {
            return Optional.empty();
        }
        return Optional.of(context);
    }

    public static void cleanup(ServerLevel level) {
        Long2ObjectOpenHashMap<HarvestContext> contexts = CONTEXTS.get(level);
        if (contexts == null) {
            return;
        }
        long gameTime = level.getGameTime();
        contexts.values().removeIf(context -> context.expiresAtGameTime < gameTime);
        if (contexts.isEmpty()) {
            CONTEXTS.remove(level);
        }
    }

    private static Long2ObjectOpenHashMap<HarvestContext> contextMap(ServerLevel level) {
        return CONTEXTS.computeIfAbsent(level, ignored -> new Long2ObjectOpenHashMap<>());
    }

    private static long columnKey(BlockPos position) {
        return ChunkPos.asLong(position.getX(), position.getZ());
    }

    public static final class HarvestContext {
        private final ServerPlayer player;
        private final int minimumY;
        private final int maximumY;
        private final long expiresAtGameTime;
        private boolean rewardClaimed;

        private HarvestContext(
                ServerPlayer player,
                int minimumY,
                int maximumY,
                long expiresAtGameTime
        ) {
            this.player = player;
            this.minimumY = minimumY;
            this.maximumY = maximumY;
            this.expiresAtGameTime = expiresAtGameTime;
        }

        public ServerPlayer player() {
            return player;
        }

        public boolean claimReward() {
            if (rewardClaimed) {
                return false;
            }
            rewardClaimed = true;
            return true;
        }
    }
}
