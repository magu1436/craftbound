package com.magu1436.craftbound.occupations.blacksmith.crucible.menu;

import java.util.function.Consumer;

import com.magu1436.craftbound.occupations.blacksmith.material.MetalMaterialResolver;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class CrucibleInputSlot extends Slot {
    private final MetalMaterialResolver resolver;
    private final boolean clientSide;
    private final Consumer<CrucibleInputSlot> changeHandler;

    public CrucibleInputSlot(
        Container container,
        int slot,
        int x,
        int y,
        MetalMaterialResolver resolver,
        boolean clientSide,
        Consumer<CrucibleInputSlot> changeHandler
    ) {
        super(container, slot, x, y);
        this.resolver = resolver;
        this.clientSide = clientSide;
        this.changeHandler = changeHandler;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        // The server performs the authoritative definition lookup. The
        // client has no server datapack snapshot during this implementation.
        return clientSide || resolver.resolve(stack).isPresent();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        changeHandler.accept(this);
    }
}
