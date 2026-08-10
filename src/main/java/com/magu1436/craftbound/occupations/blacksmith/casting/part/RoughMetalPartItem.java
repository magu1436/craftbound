package com.magu1436.craftbound.occupations.blacksmith.casting.part;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class RoughMetalPartItem extends Item {
    public RoughMetalPartItem(Properties properties) { super(properties); }
    @Override
    public Component getName(ItemStack stack) {
        return RoughMetalPartStateService.read(stack).<Component>map(state -> {
            Item output = ForgeRegistries.ITEMS.getValue(state.outputItemId());
            Component name = output == null ? Component.literal(state.definitionId().toString())
                : output.getDescription();
            return Component.translatable("item.craftbound.rough_metal_part.named", name);
        }).orElseGet(() -> super.getName(stack));
    }
}
