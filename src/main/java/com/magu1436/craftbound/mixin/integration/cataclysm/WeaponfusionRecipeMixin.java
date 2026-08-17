package com.magu1436.craftbound.mixin.integration.cataclysm;

import com.magu1436.craftbound.integration.cataclysm.CataclysmFusionQualityService;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.crafting.WeaponfusionRecipe")
public abstract class WeaponfusionRecipeMixin {
    @Inject(
        method = {
            "assemble(Lnet/minecraft/world/Container;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;",
            "m_5874_(Lnet/minecraft/world/Container;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;"
        },
        at = @At("RETURN"),
        remap = false
    )
    private void craftbound$applyFusionQuality(
        Container container,
        RegistryAccess registryAccess,
        CallbackInfoReturnable<ItemStack> callback
    ) {
        CataclysmFusionQualityService.apply(
            container.getItem(0),
            container.getItem(1),
            callback.getReturnValue()
        );
    }
}
