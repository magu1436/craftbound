package com.magu1436.craftbound.occupations.blacksmith.assembly;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public interface QualityAssemblyRecipeView {
    ResourceLocation getId();

    List<AssemblyIngredient> assemblyIngredients();

    ItemStack result();
}
