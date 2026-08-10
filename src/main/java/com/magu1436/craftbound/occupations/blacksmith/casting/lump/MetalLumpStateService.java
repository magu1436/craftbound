package com.magu1436.craftbound.occupations.blacksmith.casting.lump;

import java.util.Optional;
import com.magu1436.craftbound.occupations.blacksmith.material.MetalMaterialDefinitions;
import com.magu1436.craftbound.registry.CraftboundItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class MetalLumpStateService {
    private MetalLumpStateService() {}
    public static Optional<MetalLumpState> read(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(CraftboundItems.SMALL_METAL_LUMP.get())) return Optional.empty();
        return MetalLumpStateCodec.read(stack);
    }
    public static Optional<ItemStack> createSmall(ResourceLocation metalId, int count, int unitsPerItem) {
        if (metalId == null || count <= 0 || count > 64 || unitsPerItem <= 0
            || MetalMaterialDefinitions.INSTANCE.get(metalId).isEmpty()) return Optional.empty();
        ItemStack stack = new ItemStack(CraftboundItems.SMALL_METAL_LUMP.get(), count);
        MetalLumpStateCodec.write(stack, new MetalLumpState(MetalLumpState.CURRENT_VERSION, metalId, unitsPerItem));
        return Optional.of(stack);
    }
}
