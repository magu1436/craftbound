package com.magu1436.craftbound.occupations.adventurer.events;

import java.util.Objects;
import java.util.UUID;

import com.magu1436.craftbound.common.DurationReductionCalculator;
import com.magu1436.craftbound.registry.CraftboundAttributes;

import net.minecraft.server.level.ServerPlayer;

/**
 * 炎上時間の付与・更新を検出し、冒険者スキルによる短縮を適用する。
 */
public final class AdventurerBurningResistanceService {

    private static final double MAX_REDUCTION_RATE = 0.4D;
    private static final FireDurationTracker FIRE_DURATION_TRACKER =
        new FireDurationTracker();

    private AdventurerBurningResistanceService() {
    }

    /**
     * 現在の炎上時間を初期値として記録する。
     */
    public static void initialize(ServerPlayer player) {
        Objects.requireNonNull(player, "player is null");

        FIRE_DURATION_TRACKER.initialize(
            player.getUUID(),
            getNonNegativeFireTicks(player)
        );
    }

    /**
     * 炎上時間が新規設定または延長された場合に短縮する。
     */
    public static void reduceUpdatedDuration(ServerPlayer player) {
        Objects.requireNonNull(player, "player is null");

        UUID playerId = player.getUUID();
        int currentFireTicks = getNonNegativeFireTicks(player);

        if (!FIRE_DURATION_TRACKER.isTracking(playerId)) {
            FIRE_DURATION_TRACKER.initialize(
                playerId,
                currentFireTicks
            );
            return;
        }

        if (currentFireTicks <= 0) {
            FIRE_DURATION_TRACKER.record(playerId, 0);
            return;
        }

        if (
            FIRE_DURATION_TRACKER.isDurationUpdated(
                playerId,
                currentFireTicks
            )
        ) {
            double reductionRate = player.getAttributeValue(
                Objects.requireNonNull(
                    CraftboundAttributes.BURNING_RESISTANCE.get()
                )
            );

            if (reductionRate > 0.0D) {
                int reducedFireTicks = DurationReductionCalculator.reduce(
                    currentFireTicks,
                    reductionRate,
                    MAX_REDUCTION_RATE
                );

                player.setRemainingFireTicks(reducedFireTicks);
                currentFireTicks = reducedFireTicks;
            }
        }

        FIRE_DURATION_TRACKER.record(playerId, currentFireTicks);
    }

    /**
     * プレイヤーの一時的な追跡状態を破棄する。
     */
    public static void remove(ServerPlayer player) {
        Objects.requireNonNull(player, "player is null");
        FIRE_DURATION_TRACKER.remove(player.getUUID());
    }

    private static int getNonNegativeFireTicks(ServerPlayer player) {
        return Math.max(player.getRemainingFireTicks(), 0);
    }
}
