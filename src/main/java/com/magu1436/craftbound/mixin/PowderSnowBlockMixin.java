package com.magu1436.craftbound.mixin;

import com.magu1436.craftbound.occupations.explorer.PowderSnowTraversalService;
import com.magu1436.craftbound.occupations.explorer.TraversalMomentumAccess;
import com.magu1436.craftbound.occupations.explorer.TraversalSlowdownAdjustment;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 粉雪がEntityへ適用する水平移動の減速係数だけを補正する。
 */
@Mixin(PowderSnowBlock.class)
public abstract class PowderSnowBlockMixin {

    @Redirect(
        method = "entityInside",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;"
                + "makeStuckInBlock("
                + "Lnet/minecraft/world/level/block/state/BlockState;"
                + "Lnet/minecraft/world/phys/Vec3;)V"
        )
    )
    private void craftbound$adjustPowderSnowSpeedFactors(
        Entity entity,
        BlockState state,
        Vec3 vanillaFactors
    ) {
        TraversalSlowdownAdjustment adjustment =
            PowderSnowTraversalService.createAdjustment(
                entity,
                vanillaFactors
            );
        entity.makeStuckInBlock(
            state,
            adjustment.speedFactors()
        );
        ((TraversalMomentumAccess) entity)
            .craftbound$setHorizontalMomentumRetention(
                adjustment.horizontalMomentumRetention()
            );
    }
}
