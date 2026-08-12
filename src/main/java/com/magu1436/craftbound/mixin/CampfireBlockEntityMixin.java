package com.magu1436.craftbound.mixin;

import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityData;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 焚き火の入力品質を出力へ継承し、途中で腐敗した入力の進行を止める。 */
@Mixin(CampfireBlockEntity.class)
public abstract class CampfireBlockEntityMixin {

    @Shadow
    @Final
    private NonNullList<ItemStack> items;

    @Shadow
    @Final
    private int[] cookingProgress;

    @Shadow
    @Final
    private int[] cookingTime;

    @Inject(method = "cookTick", at = @At("HEAD"))
    private static void craftbound$advanceQualityClocks(
            Level level,
            BlockPos position,
            BlockState state,
            CampfireBlockEntity campfire,
            CallbackInfo callback
    ) {
        CampfireBlockEntityMixin self = (CampfireBlockEntityMixin) (Object) campfire;
        for (int slot = 0; slot < self.items.size(); slot++) {
            ItemStack input = self.items.get(slot);
            if (input.isEmpty()) {
                continue;
            }
            FoodQualityData.advanceLoadedTime(input, level.getGameTime(), 1.0D);
            if (FoodQualityData.isSpoiled(input)) {
                self.cookingProgress[slot] = 0;
            }
        }
    }

    @Inject(method = "placeFood", at = @At("HEAD"), cancellable = true)
    private void craftbound$rejectSpoiledFood(
            @Nullable Entity entity,
            ItemStack input,
            int cookingTime,
            CallbackInfoReturnable<Boolean> callback
    ) {
        CampfireBlockEntity campfire = (CampfireBlockEntity) (Object) this;
        if (campfire.getLevel() instanceof ServerLevel level) {
            FoodQualityData.advanceLoadedTime(input, level.getGameTime(), 1.0D);
        }
        if (FoodQualityData.isSpoiled(input)) {
            callback.setReturnValue(false);
        }
    }

    @Redirect(
            method = "cookTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/Containers;dropItemStack(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V"
            )
    )
    private static void craftbound$dropQualityOutput(
            Level level,
            double x,
            double y,
            double z,
            ItemStack output
    ) {
        if (level.getBlockEntity(BlockPos.containing(x, y, z)) instanceof CampfireBlockEntity campfire) {
            CampfireBlockEntityMixin self = (CampfireBlockEntityMixin) (Object) campfire;
            for (int slot = 0; slot < self.items.size(); slot++) {
                ItemStack input = self.items.get(slot);
                if (input.isEmpty() || FoodQualityData.isSpoiled(input)) {
                    continue;
                }
                if (self.cookingProgress[slot] < self.cookingTime[slot]) {
                    continue;
                }
                boolean matchingRecipe = campfire.getCookableRecipe(input)
                        .map(recipe -> ItemStack.isSameItem(recipe.getResultItem(level.registryAccess()), output))
                        .orElse(false);
                if (matchingRecipe && FoodQualityItems.isQualityTarget(output)) {
                    FoodQualityData.initialize(
                            output,
                            FoodQualityData.getOrStandard(input),
                            level.getGameTime()
                    );
                    break;
                }
            }
        }
        Containers.dropItemStack(level, x, y, z, output);
    }
}
