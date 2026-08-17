package com.magu1436.craftbound.integration.recipe;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class RecipeRemovalRegistryTest {
    @Test
    void removesOnlyRegisteredRecipeIds() {
        ResourceLocation single = new ResourceLocation("craftbound_test", "single");
        ResourceLocation first = new ResourceLocation("craftbound_test", "first");
        ResourceLocation second = new ResourceLocation("craftbound_test", "second");
        ResourceLocation untouched = new ResourceLocation("craftbound_test", "untouched");

        RecipeRemovalRegistry.register(single);
        RecipeRemovalRegistry.registerAll(List.of(first, second));

        assertTrue(RecipeRemovalRegistry.shouldRemove(single));
        assertTrue(RecipeRemovalRegistry.shouldRemove(first));
        assertTrue(RecipeRemovalRegistry.shouldRemove(second));
        assertFalse(RecipeRemovalRegistry.shouldRemove(untouched));
    }
}
