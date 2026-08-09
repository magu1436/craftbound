package com.magu1436.craftbound.occupations.blacksmith.crucible.menu;

import com.magu1436.craftbound.occupations.blacksmith.crucible.CrucibleStateService;
import com.magu1436.craftbound.registry.CraftboundItems;
import com.magu1436.craftbound.registry.CraftboundMenus;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class CrucibleMenu extends AbstractContainerMenu {
    private static final int MIN_HOTBAR_SLOT = 0;
    private static final int MAX_HOTBAR_SLOT = 8;

    private final Inventory playerInventory;
    private final int targetHotbarSlot;
    private final ItemStack initialCrucibleReference;

    public CrucibleMenu(
        int containerId,
        Inventory playerInventory,
        FriendlyByteBuf additionalData
    ) {
        this(containerId, playerInventory, additionalData.readVarInt());
    }

    public CrucibleMenu(
        int containerId,
        Inventory playerInventory,
        int targetHotbarSlot
    ) {
        super(CraftboundMenus.CRUCIBLE.get(), containerId);
        this.playerInventory = playerInventory;
        this.targetHotbarSlot = targetHotbarSlot;
        this.initialCrucibleReference = isHotbarSlot(targetHotbarSlot)
            ? playerInventory.getItem(targetHotbarSlot)
            : ItemStack.EMPTY;
    }

    public int getTargetHotbarSlot() {
        return targetHotbarSlot;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!player.isAlive() || !isHotbarSlot(targetHotbarSlot)) {
            return false;
        }

        ItemStack current = playerInventory.getItem(targetHotbarSlot);
        return current == initialCrucibleReference
            && current.is(CraftboundItems.CRUCIBLE.get())
            && CrucibleStateService.read(current).isPresent();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private static boolean isHotbarSlot(int slot) {
        return slot >= MIN_HOTBAR_SLOT && slot <= MAX_HOTBAR_SLOT;
    }
}
