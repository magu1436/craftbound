package com.magu1436.craftbound.client.carving;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public interface CarvingMaterialTextureProvider {
    CarvingMaterialColorMap colorMap(ItemStack material, ResourceLocation overrideTexture);
    void clearCache();
}
