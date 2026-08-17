package com.magu1436.craftbound.occupations.blacksmith.assembly;

import com.magu1436.craftbound.common.quality.QualityStateService;
import com.magu1436.craftbound.registry.CraftboundRecipeSerializers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
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

public final class QualityAssemblyRecipe implements CraftingRecipe, QualityAssemblyRecipeView {
    private final ResourceLocation id;
    private final String group;
    private final CraftingBookCategory category;
    private final int width;
    private final int height;
    private final List<AssemblyIngredient> ingredients;
    private final ItemStack result;

    public QualityAssemblyRecipe(ResourceLocation id, String group, CraftingBookCategory category,
        int width, int height, List<AssemblyIngredient> ingredients, ItemStack result) {
        if (width < 1 || width > 3 || height < 1 || height > 3
            || ingredients.size() != width * height || result.isEmpty()) {
            throw new IllegalArgumentException("invalid quality assembly recipe dimensions or result");
        }
        this.id = id;
        this.group = group;
        this.category = category;
        this.width = width;
        this.height = height;
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
        if (match == null) return assembled;

        List<Integer> qualities = new ArrayList<>();
        for (int recipeY = 0; recipeY < height; recipeY++) {
            for (int recipeX = 0; recipeX < width; recipeX++) {
                AssemblyIngredient ingredient = ingredientAt(recipeX, recipeY, match.mirrored());
                if (ingredient != null && ingredient.contributesToQuality()) {
                    ItemStack input = container.getItem(
                        recipeX + match.offsetX() + (recipeY + match.offsetY()) * container.getWidth());
                    QualityStateService.read(input).ifPresent(state -> qualities.add(state.quality()));
                }
            }
        }
        OptionalInt quality = FinishedItemQualityCalculator.calculate(qualities);
        quality.ifPresent(value -> QualityStateService.setQuality(assembled, value));
        return assembled;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= this.width && height >= this.height;
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
        return CraftboundRecipeSerializers.QUALITY_ASSEMBLY.get();
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
            AssemblyIngredient ingredient = ingredients.get(index);
            if (ingredient instanceof VanillaAssemblyIngredient vanilla) {
                display.set(index, vanilla.ingredient());
            }
        }
        return display;
    }

    public int width() { return width; }
    public int height() { return height; }
    public List<AssemblyIngredient> assemblyIngredients() { return ingredients; }
    public ItemStack result() { return result.copy(); }

    private Match findMatch(CraftingContainer container) {
        for (int offsetY = 0; offsetY <= container.getHeight() - height; offsetY++) {
            for (int offsetX = 0; offsetX <= container.getWidth() - width; offsetX++) {
                if (matchesAt(container, offsetX, offsetY, false)) return new Match(offsetX, offsetY, false);
                if (matchesAt(container, offsetX, offsetY, true)) return new Match(offsetX, offsetY, true);
            }
        }
        return null;
    }

    private boolean matchesAt(CraftingContainer container, int offsetX, int offsetY, boolean mirrored) {
        for (int gridY = 0; gridY < container.getHeight(); gridY++) {
            for (int gridX = 0; gridX < container.getWidth(); gridX++) {
                int recipeX = gridX - offsetX;
                int recipeY = gridY - offsetY;
                AssemblyIngredient ingredient = recipeX >= 0 && recipeX < width
                    && recipeY >= 0 && recipeY < height
                    ? ingredientAt(recipeX, recipeY, mirrored) : null;
                ItemStack stack = container.getItem(gridX + gridY * container.getWidth());
                if (ingredient == null ? !stack.isEmpty() : !ingredient.matches(stack)) return false;
            }
        }
        return true;
    }

    private AssemblyIngredient ingredientAt(int x, int y, boolean mirrored) {
        int actualX = mirrored ? width - 1 - x : x;
        return ingredients.get(actualX + y * width);
    }

    private record Match(int offsetX, int offsetY, boolean mirrored) {}
}
