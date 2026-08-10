package com.magu1436.craftbound.occupations.blacksmith.client;

import com.magu1436.craftbound.occupations.blacksmith.casting.part.RoughMetalPartStateService;
import net.minecraft.world.item.ItemStack;

public final class RoughMetalPartColorHandler {
    private static final int WHITE = 0xFFFFFF;
    private RoughMetalPartColorHandler() {}

    public static int getColor(ItemStack stack, int tintIndex) {
        if (tintIndex != 0) return WHITE;
        return RoughMetalPartStateService.read(stack)
            .map(state -> MetalRenderColorResolver.resolve(state.visualData()))
            .orElse(WHITE);
    }
}
