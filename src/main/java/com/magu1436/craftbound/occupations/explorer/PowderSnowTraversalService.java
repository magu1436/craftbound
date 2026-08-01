package com.magu1436.craftbound.occupations.explorer;

import com.magu1436.craftbound.registry.CraftboundAttributes;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 粉雪踏破による粉雪内の水平移動減速軽減を適用する。
 */
public final class PowderSnowTraversalService {

    private PowderSnowTraversalService() {
    }

    public static TraversalSlowdownAdjustment createAdjustment(
        Entity entity,
        Vec3 vanillaFactors
    ) {
        if (!(entity instanceof Player player)) {
            return new TraversalSlowdownAdjustment(vanillaFactors, 0.0D);
        }

        double reduction = player.getAttributeValue(
            CraftboundAttributes.POWDER_SNOW_TRAVERSAL.get()
        );
        return TraversalSlowdownCalculator.createAdjustment(
            vanillaFactors,
            reduction,
            false
        );
    }
}
