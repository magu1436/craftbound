package com.magu1436.craftbound.occupations.explorer;

import com.magu1436.craftbound.registry.CraftboundAttributes;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * 魂砂踏破によるソウルサンドの移動減速軽減を適用する。
 */
public final class SoulSandTraversalService {

    private SoulSandTraversalService() {
    }

    public static float adjustSpeedFactor(
        Entity entity,
        Block block,
        float vanillaFactor
    ) {
        if (!(entity instanceof Player player)
            || block != Blocks.SOUL_SAND) {
            return vanillaFactor;
        }

        double reduction = player.getAttributeValue(
            CraftboundAttributes.SOUL_SAND_TRAVERSAL.get()
        );
        return TraversalSlowdownCalculator.adjustFactor(
            vanillaFactor,
            reduction
        );
    }
}
