package com.magu1436.craftbound.occupations.blacksmith.assembly;

import net.minecraft.world.item.ItemStack;

public sealed interface AssemblyIngredient permits VanillaAssemblyIngredient,
    MetalPartAssemblyIngredient, NonMetalPartAssemblyIngredient {

    boolean matches(ItemStack stack);

    boolean contributesToQuality();
}
