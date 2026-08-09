package com.magu1436.craftbound.occupations.blacksmith.casting.lump;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class MetalLumpItem extends Item {
    public MetalLumpItem(Properties properties) { super(properties); }
    @Override
    public Component getName(ItemStack stack) {
        return MetalLumpStateService.read(stack)
            .<Component>map(state -> Component.translatable("item.craftbound.small_metal_lump.named",
                Component.translatable("metal.craftbound." + state.metalId().getPath().replace('/', '.'))))
            .orElseGet(() -> super.getName(stack));
    }
}
