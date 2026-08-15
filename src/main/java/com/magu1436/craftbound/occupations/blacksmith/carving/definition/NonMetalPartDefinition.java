package com.magu1436.craftbound.occupations.blacksmith.carving.definition;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record NonMetalPartDefinition(ResourceLocation id, ResourceLocation materialProfileId,
    Item ingredientItem, TagKey<Item> ingredientTag, int ingredientCount,
    ResourceLocation outputItemId, ResourceLocation shapeId, int baseGridSize,
    double warningRetention, ResourceLocation breakEvaluatorId, double breakThreshold,
    ResourceLocation shapeEvaluatorId) {
    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && (ingredientItem != null ? stack.is(ingredientItem) : stack.is(ingredientTag));
    }
}
