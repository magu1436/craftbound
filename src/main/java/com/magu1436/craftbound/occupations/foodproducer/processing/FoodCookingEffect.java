package com.magu1436.craftbound.occupations.foodproducer.processing;

import net.minecraft.resources.ResourceLocation;

/** 完成料理へスナップショット保存する状態効果1件分。 */
public record FoodCookingEffect(
        ResourceLocation id,
        int amplifier,
        int durationTicks,
        boolean showParticles,
        boolean showIcon
) {

    public FoodCookingEffect {
        if (id == null) {
            throw new IllegalArgumentException("effect id is null");
        }
        amplifier = Math.max(0, amplifier);
        durationTicks = Math.max(0, durationTicks);
    }
}
