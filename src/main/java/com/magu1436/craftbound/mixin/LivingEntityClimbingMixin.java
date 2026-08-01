package com.magu1436.craftbound.mixin;

import com.magu1436.craftbound.occupations.explorer.ClimbingService;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * バニラが計算した登攀時の垂直移動速度だけを補正する。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityClimbingMixin {

    @Shadow
    protected boolean jumping;

    @ModifyArg(
        method = "handleOnClimbable"
            + "(Lnet/minecraft/world/phys/Vec3;)"
            + "Lnet/minecraft/world/phys/Vec3;",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/Math;max(DD)D"
        ),
        index = 1
    )
    private double craftbound$increaseClimbingDescentSpeed(
        double vanillaMinimumY
    ) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof Player player)) {
            return vanillaMinimumY;
        }

        double bonus = ClimbingService.getSpeedBonus(player);
        return vanillaMinimumY * (1.0D + bonus);
    }

    @Inject(
        method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
        at = @At("TAIL")
    )
    private void craftbound$increaseClimbingAscentSpeed(
        Vec3 movementInput,
        CallbackInfo callbackInfo
    ) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity instanceof Player player)) {
            return;
        }

        double bonus = ClimbingService.getSpeedBonus(player);
        if (bonus <= 0.0D) {
            return;
        }

        boolean hasHorizontalInput =
            movementInput.horizontalDistanceSqr() > 1.0E-7D;
        boolean activelyClimbing =
            this.jumping || player.horizontalCollision && hasHorizontalInput;
        Vec3 velocity = player.getDeltaMovement();
        if (!activelyClimbing || velocity.y <= 0.0D) {
            return;
        }

        player.setDeltaMovement(
            velocity.x,
            velocity.y * (1.0D + bonus),
            velocity.z
        );
    }
}
