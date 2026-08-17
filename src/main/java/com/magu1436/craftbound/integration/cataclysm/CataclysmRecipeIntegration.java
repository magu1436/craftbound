package com.magu1436.craftbound.integration.cataclysm;

import com.magu1436.craftbound.integration.recipe.RecipeRemovalRegistry;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

public final class CataclysmRecipeIntegration {
    private CataclysmRecipeIntegration() {}

    public static void register() {
        RecipeRemovalRegistry.registerAll(List.of(
            id("blazing_grips"),
            id("smithing/ignitium_helmet"),
            id("smithing/ignitium_chestplate"),
            id("smithing/ignitium_leggings"),
            id("smithing/ignitium_boots")
        ));
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("cataclysm", path);
    }
}
