package com.magu1436.craftbound.occupations.blacksmith.casting.lump;

import java.util.Optional;
import com.magu1436.craftbound.occupations.blacksmith.material.MetalMaterialDefinitions;
import com.magu1436.craftbound.registry.CraftboundItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class MetalLumpStateService {
    private MetalLumpStateService() {}
    public static Optional<MetalLumpState> read(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof MetalLumpItem)) return Optional.empty();
        return MetalLumpStateCodec.read(stack);
    }

    public static Optional<ItemStack> create(
        ResourceLocation lumpItemId,
        ResourceLocation metalId,
        int count,
        int unitsPerItem
    ) {
        if (lumpItemId == null || metalId == null || count <= 0 || count > 64
            || unitsPerItem <= 0 || MetalMaterialDefinitions.INSTANCE.get(metalId).isEmpty()) {
            return Optional.empty();
        }
        Item item = ForgeRegistries.ITEMS.getValue(lumpItemId);
        if (!(item instanceof MetalLumpItem)) return Optional.empty();

        ItemStack stack = new ItemStack(item, count);
        if (count > stack.getMaxStackSize()) return Optional.empty();
        MetalLumpStateCodec.write(
            stack,
            new MetalLumpState(MetalLumpState.CURRENT_VERSION, metalId, unitsPerItem)
        );
        return read(stack).isPresent() ? Optional.of(stack) : Optional.empty();
    }

    public static Optional<ItemStack> createSmall(ResourceLocation metalId, int count, int unitsPerItem) {
        return create(
            CraftboundItems.SMALL_METAL_LUMP.getId(),
            metalId,
            count,
            unitsPerItem
        );
    }
}
