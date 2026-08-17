package com.magu1436.craftbound.occupations.blacksmith.assembly;

import com.magu1436.craftbound.common.quality.QualityStateService;
import com.magu1436.craftbound.registry.CraftboundRecipeSerializers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.BiPredicate;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class QualityShapelessAssemblyRecipe
    implements CraftingRecipe, QualityAssemblyRecipeView {
    private final ResourceLocation id;
    private final String group;
    private final CraftingBookCategory category;
    private final List<AssemblyIngredient> ingredients;
    private final ItemStack result;

    public QualityShapelessAssemblyRecipe(ResourceLocation id, String group,
        CraftingBookCategory category, List<AssemblyIngredient> ingredients, ItemStack result) {
        if (ingredients.isEmpty() || ingredients.size() > 9 || result.isEmpty()) {
            throw new IllegalArgumentException("invalid quality shapeless assembly ingredients or result");
        }
        this.id = id;
        this.group = group;
        this.category = category;
        this.ingredients = Collections.unmodifiableList(new ArrayList<>(ingredients));
        this.result = result.copy();
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return findMatch(container) != null;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        ItemStack assembled = result.copy();
        Match match = findMatch(container);
        if (match == null) {
            return assembled;
        }

        List<Integer> qualities = new ArrayList<>();
        for (int ingredientIndex = 0; ingredientIndex < ingredients.size(); ingredientIndex++) {
            if (ingredients.get(ingredientIndex).contributesToQuality()) {
                ItemStack input = container.getItem(match.containerSlots()[ingredientIndex]);
                QualityStateService.read(input).ifPresent(state -> qualities.add(state.quality()));
            }
        }
        OptionalInt quality = FinishedItemQualityCalculator.calculate(qualities);
        quality.ifPresent(value -> QualityStateService.setQuality(assembled, value));
        return assembled;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= ingredients.size();
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return result.copy();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CraftboundRecipeSerializers.QUALITY_SHAPELESS_ASSEMBLY.get();
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public CraftingBookCategory category() {
        return category;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> display = NonNullList.withSize(ingredients.size(), Ingredient.EMPTY);
        for (int index = 0; index < ingredients.size(); index++) {
            if (ingredients.get(index) instanceof VanillaAssemblyIngredient vanilla) {
                display.set(index, vanilla.ingredient());
            }
        }
        return display;
    }

    @Override
    public List<AssemblyIngredient> assemblyIngredients() {
        return ingredients;
    }

    @Override
    public ItemStack result() {
        return result.copy();
    }

    private Match findMatch(CraftingContainer container) {
        List<ItemStack> stacks = new ArrayList<>();
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty()) {
                stacks.add(stack);
                slots.add(slot);
            }
        }
        int[] stackAssignment = findAssignment(ingredients, stacks);
        if (stackAssignment == null) {
            return null;
        }
        int[] containerSlots = new int[stackAssignment.length];
        for (int index = 0; index < stackAssignment.length; index++) {
            containerSlots[index] = slots.get(stackAssignment[index]);
        }
        return new Match(containerSlots);
    }

    static int[] findAssignment(List<AssemblyIngredient> ingredients, List<ItemStack> stacks) {
        return findAssignment(ingredients.size(), stacks.size(),
            (ingredientIndex, stackIndex) -> ingredients.get(ingredientIndex).matches(stacks.get(stackIndex)));
    }

    static int[] findAssignment(int ingredientCount, int stackCount,
        BiPredicate<Integer, Integer> matches) {
        if (ingredientCount != stackCount) {
            return null;
        }
        int[] assignment = new int[ingredientCount];
        boolean[] usedStacks = new boolean[stackCount];
        return assign(ingredientCount, stackCount, matches, 0, assignment, usedStacks)
            ? assignment : null;
    }

    private static boolean assign(int ingredientCount, int stackCount,
        BiPredicate<Integer, Integer> matches, int ingredientIndex, int[] assignment,
        boolean[] usedStacks) {
        if (ingredientIndex == ingredientCount) {
            return true;
        }
        for (int stackIndex = 0; stackIndex < stackCount; stackIndex++) {
            if (!usedStacks[stackIndex] && matches.test(ingredientIndex, stackIndex)) {
                usedStacks[stackIndex] = true;
                assignment[ingredientIndex] = stackIndex;
                if (assign(ingredientCount, stackCount, matches, ingredientIndex + 1,
                    assignment, usedStacks)) {
                    return true;
                }
                usedStacks[stackIndex] = false;
            }
        }
        return false;
    }

    private record Match(int[] containerSlots) {}
}
