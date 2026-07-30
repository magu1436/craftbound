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

    public static Vec3 adjustSpeedFactors(
        Entity entity,
        Vec3 vanillaFactors
    ) {
        if (!(entity instanceof Player player)) {
            return vanillaFactors;
        }

        double reduction = player.getAttributeValue(
            CraftboundAttributes.BUSHWHACKING.get()
        );
        return TraversalSlowdownCalculator.adjustFactors(
            vanillaFactors,
            reduction,
            false
        );
    }
}
