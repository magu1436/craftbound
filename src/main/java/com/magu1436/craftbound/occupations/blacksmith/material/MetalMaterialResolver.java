package com.magu1436.craftbound.occupations.blacksmith.material;

import java.util.Optional;

import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface MetalMaterialResolver {
    Optional<ResolvedMetalMaterial> resolve(ItemStack stack);
}
