package com.magu1436.craftbound.occupations.blacksmith.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.magu1436.craftbound.occupations.blacksmith.material.MetalVisualData;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class MetalRenderColorResolver {
    public static final int DEFAULT_COLOR = 0x9A9A9A;
    private static final Map<ResourceLocation, Integer> COLOR_CACHE = new ConcurrentHashMap<>();

    private MetalRenderColorResolver() {}

    public static int resolve(MetalVisualData data) {
        if (data.explicitRgb() != null) return data.explicitRgb();
        if (data.representativeItemId() == null) return DEFAULT_COLOR;
        return COLOR_CACHE.computeIfAbsent(
            data.representativeItemId(), MetalRenderColorResolver::calculateAverageColor);
    }

    public static void clearCache() {
        COLOR_CACHE.clear();
    }

    private static int calculateAverageColor(ResourceLocation itemId) {
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null) return DEFAULT_COLOR;
        try {
            Minecraft minecraft = Minecraft.getInstance();
            BakedModel model = minecraft.getItemRenderer().getModel(
                new ItemStack(item), null, null, 0);
            TextureAtlasSprite sprite = model.getParticleIcon();
            return average(sprite.contents());
        } catch (RuntimeException exception) {
            return DEFAULT_COLOR;
        }
    }

    private static int average(SpriteContents contents) {
        NativeImage image = contents.getOriginalImage();
        long red = 0L, green = 0L, blue = 0L, count = 0L;
        for (int y = 0; y < contents.height(); y++) {
            for (int x = 0; x < contents.width(); x++) {
                int color = image.getPixelRGBA(x, y);
                if (FastColor.ABGR32.alpha(color) == 0) continue;
                red += FastColor.ABGR32.red(color);
                green += FastColor.ABGR32.green(color);
                blue += FastColor.ABGR32.blue(color);
                count++;
            }
        }
        if (count == 0L) return DEFAULT_COLOR;
        return ((int) (red / count) << 16) | ((int) (green / count) << 8)
            | (int) (blue / count);
    }
}
