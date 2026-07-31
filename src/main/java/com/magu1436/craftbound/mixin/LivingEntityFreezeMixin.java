package com.magu1436.craftbound.mixin;

import com.magu1436.craftbound.occupations.explorer.ColdAdaptationService;

import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 凍結tickが増加する処理だけを寒冷地対応の確率判定に置き換える。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityFreezeMixin {

    @ModifyArg(
        method = "aiStep()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;"
                + "setTicksFrozen(I)V"
        ),
        index = 0
    )
    private int craftbound$reduceFrozenTickIncrease(int frozenTicks) {
        return ColdAdaptationService.reduceFrozenTicks(
            (LivingEntity) (Object) this,
            frozenTicks
        );
    }
}
