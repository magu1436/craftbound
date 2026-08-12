package com.magu1436.craftbound.mixin;

import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQuality;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityData;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityItems;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** かまど系ブロックの出力へ入力品質を継承し、腐敗入力を停止する. */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

    @Unique
    @Nullable
    private FoodQuality craftbound$pendingQuality;

    @Unique
    private boolean craftbound$outputWasEmpty;

    @Inject(method = "canBurn", at = @At("RETURN"), cancellable = true)
    private void craftbound$checkQualityCompatibility(
            RegistryAccess registryAccess,
            @Nullable Recipe<?> recipe,
            NonNullList<ItemStack> items,
            int maximumStackSize,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (!callback.getReturnValue() || recipe == null) {
            return;
        }

        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) (Object) this;
        Level level = furnace.getLevel();
        ItemStack input = items.get(0);
        if (level instanceof ServerLevel serverLevel) {
            FoodQualityData.advanceLoadedTime(input, serverLevel.getGameTime(), 1.0D);
        }
        if (FoodQualityData.isSpoiled(input)) {
            callback.setReturnValue(false);
            return;
        }

        ItemStack recipeOutput = recipe.getResultItem(registryAccess);
        if (!FoodQualityItems.isQualityTarget(recipeOutput)
                || !FoodQualityItems.isQualityTarget(input)
                || items.get(2).isEmpty()
                || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        ItemStack existingOutput = items.get(2);
        FoodQualityData.advanceLoadedTime(existingOutput, serverLevel.getGameTime(), 1.0D);
        ItemStack candidate = recipeOutput.copy();
        FoodQualityData.initialize(candidate, FoodQualityData.getOrStandard(input), serverLevel.getGameTime());
        if (!FoodQualityData.isMergeCompatible(existingOutput, candidate)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "burn", at = @At("HEAD"))
    private void craftbound$captureInputQuality(
            RegistryAccess registryAccess,
            @Nullable Recipe<?> recipe,
            NonNullList<ItemStack> items,
            int maximumStackSize,
            CallbackInfoReturnable<Boolean> callback
    ) {
        craftbound$pendingQuality = null;
        craftbound$outputWasEmpty = items.get(2).isEmpty();
        if (recipe != null
                && FoodQualityItems.isQualityTarget(items.get(0))
                && FoodQualityItems.isQualityTarget(recipe.getResultItem(registryAccess))) {
            craftbound$pendingQuality = FoodQualityData.getOrStandard(items.get(0));
        }
    }

    @Inject(method = "burn", at = @At("RETURN"))
    private void craftbound$applyOutputQuality(
            RegistryAccess registryAccess,
            @Nullable Recipe<?> recipe,
            NonNullList<ItemStack> items,
            int maximumStackSize,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (!callback.getReturnValue()
                || !craftbound$outputWasEmpty
                || craftbound$pendingQuality == null) {
            return;
        }
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) (Object) this;
        if (furnace.getLevel() instanceof ServerLevel level) {
            FoodQualityData.initialize(
                    items.get(2),
                    craftbound$pendingQuality,
                    level.getGameTime()
            );
        }
    }
}
