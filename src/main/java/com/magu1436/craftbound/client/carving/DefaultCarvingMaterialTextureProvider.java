package com.magu1436.craftbound.client.carving;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class DefaultCarvingMaterialTextureProvider implements CarvingMaterialTextureProvider {
    public static final DefaultCarvingMaterialTextureProvider INSTANCE = new DefaultCarvingMaterialTextureProvider();
    private static final int SIZE = 16;
    private final Map<Key, CarvingMaterialColorMap> cache = new HashMap<>();
    private DefaultCarvingMaterialTextureProvider() {}
    @Override public CarvingMaterialColorMap colorMap(ItemStack material, ResourceLocation overrideTexture) {
        Key key = new Key(ForgeRegistries.ITEMS.getKey(material.getItem()), overrideTexture);
        return cache.computeIfAbsent(key, ignored -> create(material, overrideTexture));
    }
    @Override public void clearCache() { cache.clear(); }
    private static CarvingMaterialColorMap create(ItemStack stack, ResourceLocation override) {
        Minecraft minecraft = Minecraft.getInstance();
        TextureAtlasSprite sprite = override == null
            ? minecraft.getItemRenderer().getModel(stack, null, null, 0).getParticleIcon()
            : minecraft.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).getSprite(override);
        int width = sprite.contents().width(), height = sprite.contents().height();
        int[] colors = new int[SIZE * SIZE]; boolean[] opaque = new boolean[colors.length];
        for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) {
            int pixel = sprite.contents().getOriginalImage().getPixelRGBA(
                Math.min(width - 1, x * width / SIZE), Math.min(height - 1, y * height / SIZE));
            int alpha = pixel >>> 24;
            int red = pixel & 0xFF, green = pixel >>> 8 & 0xFF, blue = pixel >>> 16 & 0xFF;
            colors[y * SIZE + x] = red << 16 | green << 8 | blue;
            opaque[y * SIZE + x] = alpha > 0;
        }
        for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) {
            int index = y * SIZE + x; if (opaque[index]) continue;
            int best = -1, bestDistance = Integer.MAX_VALUE;
            for (int cy = 0; cy < SIZE; cy++) for (int cx = 0; cx < SIZE; cx++) {
                int candidate = cy * SIZE + cx; if (!opaque[candidate]) continue;
                int distance = (cx - x) * (cx - x) + (cy - y) * (cy - y);
                if (distance < bestDistance) { bestDistance = distance; best = candidate; }
            }
            colors[index] = best < 0 ? 0x8B5A2B : colors[best];
        }
        return new CarvingMaterialColorMap(SIZE, colors);
    }
    private record Key(ResourceLocation itemId, ResourceLocation overrideTexture) {}
}
