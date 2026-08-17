package com.magu1436.craftbound.occupations.blacksmith.quality;

import com.magu1436.craftbound.occupations.blacksmith.assembly.QualityAssemblyRecipe;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.CraftingRecipe;

public final class QualityTargetRegistry {
    private static volatile Set<Item> targets = Set.of();

    private QualityTargetRegistry() {}

    public static void rebuild(RecipeManager recipeManager) {
        replace(collectTargets(recipeManager.getAllRecipesFor(RecipeType.CRAFTING)));
    }

    static Set<Item> collectTargets(List<? extends CraftingRecipe> recipes) {
        Set<Item> rebuilt = new HashSet<>();
        recipes.stream()
            .filter(QualityAssemblyRecipe.class::isInstance)
            .map(QualityAssemblyRecipe.class::cast)
            .map(QualityAssemblyRecipe::result)
            .filter(stack -> !stack.isEmpty())
            .map(ItemStack::getItem)
            .forEach(rebuilt::add);
        return Set.copyOf(rebuilt);
    }

    public static boolean contains(ItemStack stack) {
        return !stack.isEmpty() && contains(stack.getItem());
    }

    public static boolean contains(Item item) {
        return targets.contains(item);
    }

    static void replace(Set<Item> rebuilt) {
        targets = immutableCopy(rebuilt);
    }

    static <T> Set<T> immutableCopy(Set<T> values) {
        return Set.copyOf(values);
    }
}
