package com.magu1436.craftbound.occupations.explorer;

import com.magu1436.craftbound.registry.CraftboundAttributes;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 藪漕ぎによるスイートベリーの茂みの移動減速軽減を適用する。
 */
public final class BushwhackingService {

    private BushwhackingService() {
    }

    public static TraversalSlowdownAdjustment createAdjustment(
        Entity entity,
        Vec3 vanillaFactors
    ) {
        if (!(entity instanceof Player player)) {
            return new TraversalSlowdownAdjustment(vanillaFactors, 0.0D);
        }

        double reduction = player.getAttributeValue(
            CraftboundAttributes.BUSHWHACKING.get()
        );
        return TraversalSlowdownCalculator.createAdjustment(
            vanillaFactors,
            reduction,
            false
        );
    }
}
