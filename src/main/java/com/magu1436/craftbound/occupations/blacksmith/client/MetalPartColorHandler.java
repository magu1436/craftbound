package com.magu1436.craftbound.occupations.blacksmith.client;

import com.magu1436.craftbound.occupations.blacksmith.casting.finished.MetalPartStateService;
import net.minecraft.world.item.ItemStack;

public final class MetalPartColorHandler {
    private static final int WHITE = 0xFFFFFF;

    private MetalPartColorHandler() {}

    public static int getColor(ItemStack stack, int tintIndex) {
        if (tintIndex != 0) return WHITE;
        return MetalPartStateService.read(stack)
            .map(state -> MetalRenderColorResolver.resolve(state.visualData()))
            .orElse(WHITE);
    }
}
