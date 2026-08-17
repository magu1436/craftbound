package com.magu1436.craftbound.mixin;

import com.google.gson.JsonElement;
import com.magu1436.craftbound.integration.recipe.RecipeRemovalRegistry;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {
    @ModifyVariable(
        method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private Map<ResourceLocation, JsonElement> craftbound$removeRegisteredRecipes(
        Map<ResourceLocation, JsonElement> recipes
    ) {
        Map<ResourceLocation, JsonElement> filtered = new HashMap<>(recipes);
        filtered.keySet().removeIf(RecipeRemovalRegistry::shouldRemove);
        return filtered;
    }
}
