package com.magu1436.craftbound.occupations.architect.experience;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import com.magu1436.craftbound.occupations.architect.capability.PendingConstruction;

/**
 * 期限を迎えた施工候補だけを1秒単位で処理する。
 */
public final class ConstructionScheduler {
    private static final int WHEEL_SIZE = 120;
    private static final Map<ServerLevel, TimingWheel> WHEELS =
        new IdentityHashMap<>();

    private ConstructionScheduler() {
    }

    public static void schedule(
        ServerLevel level,
        BlockPos pos,
        PendingConstruction pending
    ) {
        long currentSecond = level.getGameTime() / 20L;
        long matureSecond = toCeilingSecond(pending.matureAtGameTime());
        long scheduledSecond = Math.max(matureSecond, currentSecond + 1L);
        int index = (int) (scheduledSecond % WHEEL_SIZE);
        getWheel(level).buckets[index].add(
            new ScheduledConstruction(
                pos.immutable(),
                pending.matureAtGameTime()
            )
        );
    }

    public static void tick(ServerLevel level) {
        long gameTime = level.getGameTime();
        if (gameTime % 20L != 0L) {
            return;
        }

        TimingWheel wheel = WHEELS.get(level);
        if (wheel == null) {
            return;
        }
        int index = (int) ((gameTime / 20L) % WHEEL_SIZE);
        List<ScheduledConstruction> due = wheel.take(index);
        for (ScheduledConstruction scheduled : due) {
            if (scheduled.matureAtGameTime() > gameTime) {
                reschedule(level, scheduled);
                continue;
            }
            if (!level.hasChunkAt(scheduled.pos())) {
                continue;
            }
            ConstructionConfirmationService.confirm(
                level,
                scheduled.pos(),
                scheduled.matureAtGameTime()
            );
        }
    }

    public static void clear(ServerLevel level) {
        WHEELS.remove(level);
    }

    private static void reschedule(
        ServerLevel level,
        ScheduledConstruction scheduled
    ) {
        int index = (int) (
            toCeilingSecond(scheduled.matureAtGameTime()) % WHEEL_SIZE
        );
        getWheel(level).buckets[index].add(scheduled);
    }

    private static TimingWheel getWheel(ServerLevel level) {
        return WHEELS.computeIfAbsent(level, ignored -> new TimingWheel());
    }

    private static long toCeilingSecond(long gameTime) {
        return (gameTime + 19L) / 20L;
    }

    private static final class TimingWheel {
        private final List<ScheduledConstruction>[] buckets;

        @SuppressWarnings("unchecked")
        private TimingWheel() {
            buckets = (List<ScheduledConstruction>[]) new List<?>[WHEEL_SIZE];
            for (int index = 0; index < buckets.length; index++) {
                buckets[index] = new ArrayList<>();
            }
        }

        private List<ScheduledConstruction> take(int index) {
            List<ScheduledConstruction> due = buckets[index];
            buckets[index] = new ArrayList<>();
            return due;
        }
    }
}
