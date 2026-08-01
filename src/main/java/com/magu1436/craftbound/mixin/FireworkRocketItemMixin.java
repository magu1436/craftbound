package com.magu1436.craftbound.mixin;

import com.magu1436.craftbound.occupations.architect.FireworkConservationService;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * エリトラ加速用花火の消費だけを飛行技術の判定対象にする。
 */
@Mixin(FireworkRocketItem.class)
public abstract class FireworkRocketItemMixin {

    @Redirect(
        method = "use(Lnet/minecraft/world/level/Level;"
            + "Lnet/minecraft/world/entity/player/Player;"
            + "Lnet/minecraft/world/InteractionHand;)"
            + "Lnet/minecraft/world/InteractionResultHolder;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"
        )
    )
    private void craftbound$consumeOrConserveFirework(
        ItemStack stack,
        int amount,
        Level level,
        Player player,
        InteractionHand hand
    ) {
        if (!FireworkConservationService.shouldConserve(
            player,
            stack,
            amount
        )) {
            stack.shrink(amount);
        }
    }
}
