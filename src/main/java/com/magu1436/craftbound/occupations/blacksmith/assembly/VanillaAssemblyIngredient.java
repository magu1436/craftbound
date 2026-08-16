package com.magu1436.craftbound.occupations.blacksmith.assembly;

import java.util.Objects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public record VanillaAssemblyIngredient(Ingredient ingredient) implements AssemblyIngredient {
    public VanillaAssemblyIngredient {
        Objects.requireNonNull(ingredient, "ingredient");
    }

    @Override
    public boolean matches(ItemStack stack) {
        return ingredient.test(stack);
    }

    @Override
    public boolean contributesToQuality() {
        return false;
    }
}
