package com.magu1436.craftbound.mixin;

import com.magu1436.craftbound.occupations.foodproducer.ranch.RanchBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 牧畜ブロックへの搬入時だけ、品質時計NBTを考慮したスタック統合を行う。 */
@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {

    @Inject(
            method = {
                    "ejectItems(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/HopperBlockEntity;)Z",
                    "m_155562_(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/HopperBlockEntity;)Z"
            },
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void craftbound$insertFeedIntoRanch(
            Level level,
            BlockPos hopperPos,
            BlockState hopperState,
            HopperBlockEntity hopper,
            CallbackInfoReturnable<Boolean> callback
    ) {
        BlockPos destinationPos = hopperPos.relative(hopperState.getValue(HopperBlock.FACING));
        BlockEntity destination = level.getBlockEntity(destinationPos);
        if (!(destination instanceof RanchBlockEntity ranch)) {
            return;
        }

        for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
            ItemStack source = hopper.getItem(slot);
            if (!source.isEmpty() && ranch.insertOneFromHopper(source, level.getGameTime())) {
                source.shrink(1);
                hopper.setChanged();
                callback.setReturnValue(true);
                return;
            }
        }

        callback.setReturnValue(false);
    }
}
