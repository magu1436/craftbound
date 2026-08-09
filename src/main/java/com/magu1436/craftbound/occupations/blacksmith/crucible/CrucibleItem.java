package com.magu1436.craftbound.occupations.blacksmith.crucible;

import com.magu1436.craftbound.occupations.blacksmith.crucible.menu.CrucibleMenu;
import com.magu1436.craftbound.registry.CraftboundItems;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public final class CrucibleItem extends Item {

    public CrucibleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
        Level level,
        Player player,
        InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }

        int targetSlot = player.getInventory().selected;
        if (!isSelectedCrucible(player, targetSlot)) {
            return InteractionResultHolder.pass(stack);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            SimpleMenuProvider provider = new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) ->
                    new CrucibleMenu(containerId, inventory, targetSlot),
                Component.translatable("screen.craftbound.crucible")
            );
            NetworkHooks.openScreen(
                serverPlayer,
                provider,
                buffer -> buffer.writeVarInt(targetSlot)
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.PASS;
    }

    private static boolean isSelectedCrucible(Player player, int slot) {
        return slot >= 0
            && slot < 9
            && player.getInventory().getItem(slot).is(
                CraftboundItems.CRUCIBLE.get()
            )
            && CrucibleStateService.read(
                player.getInventory().getItem(slot)
            ).isPresent();
    }
}
