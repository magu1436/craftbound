package com.magu1436.craftbound.mixin;

import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityData;

import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 腐敗した材料を含む場合はクラフト結果を生成しない. */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {

    @Inject(method = "slotChangedCraftingGrid", at = @At("HEAD"), cancellable = true)
    private static void craftbound$rejectSpoiledIngredients(
            AbstractContainerMenu menu,
            Level level,
            Player player,
            CraftingContainer ingredients,
            ResultContainer result,
            CallbackInfo callback
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int slot = 0; slot < ingredients.getContainerSize(); slot++) {
            ItemStack ingredient = ingredients.getItem(slot);
            FoodQualityData.advanceLoadedTime(ingredient, serverLevel.getGameTime(), 1.0D);
            if (!FoodQualityData.isSpoiled(ingredient)) {
                continue;
            }

            clearResult(menu, result, (ServerPlayer) player);
            callback.cancel();
            return;
        }
    }

    private static void clearResult(
            AbstractContainerMenu menu,
            ResultContainer result,
            ServerPlayer player
    ) {
        result.setItem(0, ItemStack.EMPTY);
        menu.setRemoteSlot(0, ItemStack.EMPTY);
        player.connection.send(new ClientboundContainerSetSlotPacket(
                menu.containerId,
                menu.incrementStateId(),
                0,
                ItemStack.EMPTY
        ));
    }
}
