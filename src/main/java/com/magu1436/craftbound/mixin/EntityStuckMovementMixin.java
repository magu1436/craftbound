package com.magu1436.craftbound.mixin;

import com.magu1436.craftbound.occupations.explorer.TraversalMomentumAccess;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Retains horizontal momentum reduced by traversal slowdown effects.
 */
@Mixin(Entity.class)
public abstract class EntityStuckMovementMixin
    implements TraversalMomentumAccess {

    @Unique
    private double craftbound$horizontalMomentumRetention;

    @Inject(
        method = "makeStuckInBlock",
        at = @At("HEAD")
    )
    private void craftbound$clearTraversalMomentum(
        BlockState state,
        Vec3 speedFactors,
        CallbackInfo callbackInfo
    ) {
        craftbound$clearHorizontalMomentumRetention();
    }

    @ModifyArg(
        method = "move",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;"
                + "setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
            ordinal = 0
        ),
        index = 0
    )
    private Vec3 craftbound$retainTraversalMomentum(Vec3 vanillaMovement) {
        double retention = craftbound$consumeHorizontalMomentumRetention();
        if (retention <= 0.0D) {
            return vanillaMovement;
        }

        Vec3 currentMovement = ((Entity) (Object) this).getDeltaMovement();
        return new Vec3(
            currentMovement.x * retention,
            vanillaMovement.y,
            currentMovement.z * retention
        );
    }

    @Override
    public void craftbound$setHorizontalMomentumRetention(double retention) {
        craftbound$horizontalMomentumRetention = Math.max(
            0.0D,
            Math.min(retention, 1.0D)
        );
    }

    @Override
    public double craftbound$consumeHorizontalMomentumRetention() {
        double retention = craftbound$horizontalMomentumRetention;
        craftbound$horizontalMomentumRetention = 0.0D;
        return retention;
    }

    @Override
    public void craftbound$clearHorizontalMomentumRetention() {
        craftbound$horizontalMomentumRetention = 0.0D;
    }
}
