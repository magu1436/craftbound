package com.magu1436.craftbound.occupations.blacksmith.assembly;

import com.magu1436.craftbound.occupations.blacksmith.carving.definition.NonMetalMaterialDefinitions;
import com.magu1436.craftbound.occupations.blacksmith.carving.definition.NonMetalPartDefinitions;
import com.magu1436.craftbound.occupations.blacksmith.casting.definition.MetalPartDefinitions;
import com.magu1436.craftbound.occupations.blacksmith.material.MetalMaterialDefinitions;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

public final class QualityAssemblyRecipeValidationListener
    extends SimplePreparableReloadListener<Void> {

    private final RecipeManager recipeManager;

    public QualityAssemblyRecipeValidationListener(RecipeManager recipeManager) {
        this.recipeManager = recipeManager;
    }

    @Override
    protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return null;
    }

    @Override
    protected void apply(Void ignored, ResourceManager resourceManager, ProfilerFiller profiler) {
        List<String> errors = new ArrayList<>();
        recipeManager.getAllRecipesFor(RecipeType.CRAFTING).stream()
            .filter(QualityAssemblyRecipe.class::isInstance)
            .map(QualityAssemblyRecipe.class::cast)
            .forEach(recipe -> validate(recipe, errors));
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Invalid quality assembly recipes: " + String.join("; ", errors));
        }
    }

    private static void validate(QualityAssemblyRecipe recipe, List<String> errors) {
        for (AssemblyIngredient ingredient : recipe.assemblyIngredients()) {
            if (ingredient instanceof MetalPartAssemblyIngredient metal) {
                if (MetalPartDefinitions.INSTANCE.get(metal.partTypeId()).isEmpty()) {
                    errors.add(recipe.getId() + " has unknown metal part type `" + metal.partTypeId() + "`");
                }
                if (MetalMaterialDefinitions.INSTANCE.get(metal.materialId()).isEmpty()) {
                    errors.add(recipe.getId() + " has unknown metal material `" + metal.materialId() + "`");
                }
            } else if (ingredient instanceof NonMetalPartAssemblyIngredient nonMetal) {
                NonMetalPartDefinitions.INSTANCE.get(nonMetal.partTypeId()).ifPresentOrElse(definition -> {
                    if (!definition.materialProfileId().equals(nonMetal.materialId())) {
                        errors.add(recipe.getId() + " uses material `" + nonMetal.materialId()
                            + "` but part `" + nonMetal.partTypeId() + "` requires `"
                            + definition.materialProfileId() + "`");
                    }
                }, () -> errors.add(recipe.getId() + " has unknown non-metal part type `"
                    + nonMetal.partTypeId() + "`"));
                if (NonMetalMaterialDefinitions.INSTANCE.get(nonMetal.materialId()).isEmpty()) {
                    errors.add(recipe.getId() + " has unknown non-metal material `"
                        + nonMetal.materialId() + "`");
                }
            }
        }
    }
}
