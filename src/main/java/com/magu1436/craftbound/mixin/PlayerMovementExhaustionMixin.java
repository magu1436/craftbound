package com.magu1436.craftbound.mixin;

import com.magu1436.craftbound.occupations.explorer.ExpeditionEnduranceService;

import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 遠征歩行の対象となる移動由来の食料消耗だけを軽減する。
 */
@Mixin(Player.class)
public abstract class PlayerMovementExhaustionMixin {

    @ModifyArg(
        method = "checkMovementStatistics(DDD)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;"
                + "causeFoodExhaustion(F)V"
        ),
        index = 0
    )
    private float craftbound$reduceMovementExhaustion(float exhaustion) {
        return ExpeditionEnduranceService.reduceMovementExhaustion(
            (Player) (Object) this,
            exhaustion
        );
    }

    @ModifyArg(
        method = "jumpFromGround()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;"
                + "causeFoodExhaustion(F)V"
        ),
        index = 0
    )
    private float craftbound$reduceJumpExhaustion(float exhaustion) {
        return ExpeditionEnduranceService.reduceMovementExhaustion(
            (Player) (Object) this,
            exhaustion
        );
    }
}
