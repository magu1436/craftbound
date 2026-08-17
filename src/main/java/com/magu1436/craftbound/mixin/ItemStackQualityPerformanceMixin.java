package com.magu1436.craftbound.mixin;

import com.magu1436.craftbound.occupations.blacksmith.quality.BlacksmithQualityResolver;
import com.magu1436.craftbound.occupations.blacksmith.quality.QualityPerformanceService;
import com.magu1436.craftbound.occupations.blacksmith.quality.QualityPerformanceType;
import java.util.OptionalInt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackQualityPerformanceMixin {
    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void craftbound$applyQualityMiningSpeed(
        BlockState state,
        CallbackInfoReturnable<Float> callback
    ) {
        float original = callback.getReturnValue();
        if (original <= 1.0F) return;

        ItemStack stack = (ItemStack) (Object) this;
        OptionalInt quality = BlacksmithQualityResolver.resolveForPerformance(stack);
        if (quality.isEmpty()) return;
        callback.setReturnValue(QualityPerformanceService.apply(
            QualityPerformanceType.MINING_SPEED,
            original,
            quality.getAsInt()
        ));
    }

    @Inject(method = "getMaxDamage", at = @At("RETURN"), cancellable = true)
    private void craftbound$applyQualityMaxDurability(CallbackInfoReturnable<Integer> callback) {
        int original = callback.getReturnValue();
        if (original <= 0) return;

        ItemStack stack = (ItemStack) (Object) this;
        OptionalInt quality = BlacksmithQualityResolver.resolveForPerformance(stack);
        if (quality.isEmpty()) return;
        callback.setReturnValue(Math.max(
            1,
            QualityPerformanceService.applyMaxDurability(original, quality.getAsInt())
        ));
    }
}
