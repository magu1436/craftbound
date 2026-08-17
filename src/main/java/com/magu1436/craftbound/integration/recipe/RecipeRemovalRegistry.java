package com.magu1436.craftbound.integration.recipe;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;

public final class RecipeRemovalRegistry {
    private static final Set<ResourceLocation> REMOVALS = ConcurrentHashMap.newKeySet();

    private RecipeRemovalRegistry() {}

    public static void register(ResourceLocation recipeId) {
        REMOVALS.add(recipeId);
    }

    public static void registerAll(Collection<ResourceLocation> recipeIds) {
        REMOVALS.addAll(recipeIds);
    }

    public static boolean shouldRemove(ResourceLocation recipeId) {
        return REMOVALS.contains(recipeId);
    }
}
