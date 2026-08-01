package com.magu1436.craftbound.occupations.explorer;

import net.minecraft.world.phys.Vec3;

/**
 * Holds the movement adjustments for a traversal slowdown effect.
 */
public record TraversalSlowdownAdjustment(
    Vec3 speedFactors,
    double horizontalMomentumRetention
) {
}
