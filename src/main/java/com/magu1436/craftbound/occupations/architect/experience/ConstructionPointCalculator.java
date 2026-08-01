package com.magu1436.craftbound.occupations.architect.experience;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import com.magu1436.craftbound.occupations.architect.ArchitectConfig;

/**
 * 施工ポイントを固定小数点で計算する。
 */
public final class ConstructionPointCalculator {
    private static final long RATE_SCALE = 10_000L;

    private ConstructionPointCalculator() {
    }

    public static long calculate(
        ServerLevel level,
        BlockPos pos,
        BlockState state,
        int lifetimeUseCount,
        int recentUseCount
    ) {
        long pointUnits = ArchitectConfig.basePointUnits();
        pointUnits = applyRate(
            pointUnits,
            ArchitectConfig.lifetimeRate(lifetimeUseCount)
        );
        pointUnits = applyRate(
            pointUnits,
            ArchitectConfig.recentRate(recentUseCount)
        );
        return applyRate(
            pointUnits,
            ArchitectBlockPropertyClassifier.resolveRate(level, pos, state)
        );
    }

    private static long applyRate(long value, int rate) {
        return value * rate / RATE_SCALE;
    }
}
