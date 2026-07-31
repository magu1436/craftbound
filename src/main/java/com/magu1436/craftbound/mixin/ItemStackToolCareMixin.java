package com.magu1436.craftbound.mixin;

import javax.annotation.Nullable;

import com.magu1436.craftbound.occupations.explorer.ToolCareDurabilityService;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * ブロック破壊時に確定した道具の耐久消費だけを道具の手入れで抑止する。
 */
@Mixin(ItemStack.class)
public abstract class ItemStackToolCareMixin {

    @Redirect(
        method = "mineBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/Item;mineBlock("
                + "Lnet/minecraft/world/item/ItemStack;"
                + "Lnet/minecraft/world/level/Level;"
                + "Lnet/minecraft/world/level/block/state/BlockState;"
                + "Lnet/minecraft/core/BlockPos;"
                + "Lnet/minecraft/world/entity/LivingEntity;)Z"
        )
    )
    private boolean craftbound$trackBlockBreak(
        Item item,
        ItemStack stack,
        Level level,
        BlockState state,
        BlockPos position,
        LivingEntity entity
    ) {
        if (!(entity instanceof Player player)) {
            return item.mineBlock(stack, level, state, position, entity);
        }

        ToolCareDurabilityService.beginBlockBreak(player);
        try {
            return item.mineBlock(stack, level, state, position, entity);
        } finally {
            ToolCareDurabilityService.endBlockBreak();
        }
    }

    @ModifyVariable(
        method = "hurt(ILnet/minecraft/util/RandomSource;"
            + "Lnet/minecraft/server/level/ServerPlayer;)Z",
        at = @At("STORE"),
        argsOnly = true
    )
    private int craftbound$preventToolCareDurabilityConsumption(
        int amount,
        RandomSource random,
        @Nullable ServerPlayer player
    ) {
        // 耐久力エンチャントの判定後に書き戻される消費量だけを変更する。
        if (
            player != null
                && ToolCareDurabilityService
                    .shouldPreventDurabilityConsumption(player)
        ) {
            return 0;
        }
        return amount;
    }
}
