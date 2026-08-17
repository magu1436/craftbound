package com.magu1436.craftbound.mixin;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Item.class)
public abstract class ItemQualityDurabilityBarMixin {
    @Redirect(
        method = {"getBarWidth", "getBarColor"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/Item;getMaxDamage(Lnet/minecraft/world/item/ItemStack;)I",
            remap = false
        )
    )
    private int craftbound$useQualityAdjustedMaxDurability(Item item, ItemStack stack) {
        return stack.getMaxDamage();
    }
}
