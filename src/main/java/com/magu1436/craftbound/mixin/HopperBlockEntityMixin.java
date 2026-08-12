package com.magu1436.craftbound.mixin;

import com.magu1436.craftbound.occupations.foodproducer.ranch.RanchBlockEntity;
import com.magu1436.craftbound.occupations.foodproducer.processing.FoodProcessingBlockEntity;
import com.magu1436.craftbound.occupations.foodproducer.storage.PreservationStorageBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 品質対応設備への搬入と、加工設備に対するバニラホッパーの遮断を行う。 */
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
        if (destination instanceof FoodProcessingBlockEntity) {
            callback.setReturnValue(false);
            return;
        }
        if (destination instanceof PreservationStorageBlockEntity storage) {
            for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
                ItemStack source = hopper.getItem(slot);
                if (!source.isEmpty()
                        && storage.insertOneFromHopper(source)) {
                    source.shrink(1);
                    hopper.setChanged();
                    callback.setReturnValue(true);
                    return;
                }
            }
            callback.setReturnValue(false);
            return;
        }
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

    @Inject(
            method = {
                    "suckInItems(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/entity/Hopper;)Z",
                    "m_155552_(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/entity/Hopper;)Z"
            },
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void craftbound$blockExtractionFromFoodProcessor(
            Level level,
            Hopper hopper,
            CallbackInfoReturnable<Boolean> callback
    ) {
        BlockPos sourcePos = BlockPos.containing(
                hopper.getLevelX(),
                hopper.getLevelY() + 1.0D,
                hopper.getLevelZ()
        );
        if (level.getBlockEntity(sourcePos) instanceof FoodProcessingBlockEntity) {
            callback.setReturnValue(false);
        }
    }
}
