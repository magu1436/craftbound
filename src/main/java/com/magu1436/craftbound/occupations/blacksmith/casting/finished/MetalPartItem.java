package com.magu1436.craftbound.occupations.blacksmith.casting.finished;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class MetalPartItem extends Item {
    public MetalPartItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return MetalPartStateService.read(stack).<Component>map(state ->
            Component.translatable(
                "item.craftbound.metal_part.named",
                Component.translatable("metal." + state.metalId().getNamespace() + "."
                    + state.metalId().getPath().replace('/', '.')),
                Component.translatable(getDescriptionId())
            )
        ).orElseGet(() -> super.getName(stack));
    }
}
