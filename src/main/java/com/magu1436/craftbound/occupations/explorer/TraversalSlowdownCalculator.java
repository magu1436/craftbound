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

    public static TraversalSlowdownAdjustment createAdjustment(
        Vec3 vanillaFactors,
        double reduction,
        boolean adjustVertical
    ) {
        double clampedReduction = clampReduction(reduction);
        return new TraversalSlowdownAdjustment(
            adjustFactors(
                vanillaFactors,
                clampedReduction,
                adjustVertical
            ),
            clampedReduction
        );
    }

    private static double adjustFactor(
        double vanillaFactor,
        double reduction
    ) {
        if (vanillaFactor < 0.0D || vanillaFactor >= 1.0D) {
            return vanillaFactor;
        }

        double clampedReduction = clampReduction(reduction);
        return 1.0D
            - (1.0D - vanillaFactor) * (1.0D - clampedReduction);
    }

    private static double clampReduction(double reduction) {
        return Math.max(
            0.0D,
            Math.min(reduction, 1.0D)
        );
    }
}
