package com.magu1436.craftbound.mixin;

import com.magu1436.craftbound.occupations.explorer.SoulSandTraversalService;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Entityが受け取るソウルサンドの移動減速係数だけを補正する。
 */
@Mixin(Entity.class)
public abstract class EntityBlockSpeedFactorMixin {

    @Redirect(
        method = "getBlockSpeedFactor()F",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;"
                + "getSpeedFactor()F"
        )
    )
    private float craftbound$adjustSoulSandSpeedFactor(Block block) {
        float vanillaFactor = block.getSpeedFactor();
        return SoulSandTraversalService.adjustSpeedFactor(
            (Entity) (Object) this,
            block,
            vanillaFactor
        );
    }
}
