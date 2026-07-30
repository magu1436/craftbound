package com.magu1436.craftbound.occupations.explorer;

import net.minecraft.world.phys.Vec3;

/**
 * 悪路踏破系スキルによるブロックの移動減速係数を計算する。
 */
public final class TraversalSlowdownCalculator {

    private TraversalSlowdownCalculator() {
    }

    public static float adjustFactor(
        float vanillaFactor,
        double reduction
    ) {
        return (float) adjustFactor((double) vanillaFactor, reduction);
    }

    public static Vec3 adjustFactors(
        Vec3 vanillaFactors,
        double reduction,
        boolean adjustVertical
    ) {
        double x = adjustFactor(vanillaFactors.x, reduction);
        double y = adjustVertical
            ? adjustFactor(vanillaFactors.y, reduction)
            : vanillaFactors.y;
        double z = adjustFactor(vanillaFactors.z, reduction);
        return new Vec3(x, y, z);
    }

    private static double adjustFactor(
        double vanillaFactor,
        double reduction
    ) {
        if (vanillaFactor < 0.0D || vanillaFactor >= 1.0D) {
            return vanillaFactor;
        }

        double clampedReduction = Math.max(
            0.0D,
            Math.min(reduction, 1.0D)
        );
        return 1.0D
            - (1.0D - vanillaFactor) * (1.0D - clampedReduction);
    }
}
