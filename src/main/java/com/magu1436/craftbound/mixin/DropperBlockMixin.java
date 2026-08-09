package com.magu1436.craftbound.mixin;

import com.magu1436.craftbound.occupations.foodproducer.processing.FoodProcessingBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DropperBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** バニラのドロッパーをFoodProducer加工設備の搬入手段にしない。 */
@Mixin(DropperBlock.class)
public abstract class DropperBlockMixin {

    @Inject(method = "dispenseFrom", at = @At("HEAD"), cancellable = true)
    private void craftbound$blockFoodProcessorInsertion(
            ServerLevel level,
            BlockPos dropperPos,
            CallbackInfo callback
    ) {
        Direction direction = level.getBlockState(dropperPos).getValue(DispenserBlock.FACING);
        if (level.getBlockEntity(dropperPos.relative(direction)) instanceof FoodProcessingBlockEntity) {
            callback.cancel();
        }
    }
}
