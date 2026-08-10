package com.magu1436.craftbound.occupations.blacksmith.casting.finished;

import java.util.Optional;
import com.magu1436.craftbound.occupations.blacksmith.casting.part.RoughMetalPartState;
import com.magu1436.craftbound.occupations.blacksmith.casting.part.RoughMetalPartStateService;
import com.magu1436.craftbound.occupations.blacksmith.material.MetalVisualData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class MetalPartStateService {
    private MetalPartStateService() {}

    public static Optional<MetalPartState> read(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof MetalPartItem)) return Optional.empty();
        return MetalPartStateCodec.read(stack);
    }

    public static Optional<ItemStack> create(
        ResourceLocation outputItemId,
        ResourceLocation metalId,
        MetalVisualData visualData
    ) {
        if (outputItemId == null || metalId == null || visualData == null) return Optional.empty();
        Item output = ForgeRegistries.ITEMS.getValue(outputItemId);
        if (!(output instanceof MetalPartItem)) return Optional.empty();
        try {
            ItemStack stack = new ItemStack(output);
            MetalPartStateCodec.write(stack, new MetalPartState(
                MetalPartState.CURRENT_VERSION, metalId, visualData));
            return read(stack).isPresent() ? Optional.of(stack) : Optional.empty();
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    public static Optional<ItemStack> createFromRoughPart(ItemStack roughPart) {
        Optional<RoughMetalPartState> roughState = RoughMetalPartStateService.read(roughPart);
        if (roughState.isEmpty()) return Optional.empty();
        RoughMetalPartState state = roughState.get();
        return create(state.definitionSnapshot().outputItemId(), state.metalId(), state.visualData());
    }
}
