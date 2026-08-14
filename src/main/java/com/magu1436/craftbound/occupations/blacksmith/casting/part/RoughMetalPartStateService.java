package com.magu1436.craftbound.occupations.blacksmith.casting.part;

import java.util.Optional;
import java.util.UUID;
import com.magu1436.craftbound.occupations.blacksmith.casting.definition.MetalPartDefinitionSnapshot;
import com.magu1436.craftbound.occupations.blacksmith.material.MetalVisualData;
import com.magu1436.craftbound.occupations.blacksmith.material.MetalVisualDataService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.item.ItemStack;

public final class RoughMetalPartStateService {
    private RoughMetalPartStateService() {}
    public static Optional<RoughMetalPartState> read(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof RoughMetalPartItem)) return Optional.empty();
        Optional<RoughMetalPartState> result = RoughMetalPartStateCodec.read(stack);
        if (result.isEmpty()) return Optional.empty();
        ResourceLocation actualItemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return result.get().definitionSnapshot().roughOutputItemId().equals(actualItemId)
            ? result : Optional.empty();
    }
    public static Optional<ItemStack> create(UUID resultId, UUID operatorId,
        ResourceLocation metalId, MetalPartDefinitionSnapshot snapshot,
        long heatingTicks, int heatingScore, long coolingTicksAtRemoval,
        int effectiveBreakOnHit) {
        Optional<MetalVisualData> visual = MetalVisualDataService.resolve(metalId);
        if (visual.isEmpty()) return Optional.empty();
        return create(resultId, operatorId, metalId, snapshot, heatingTicks, heatingScore,
            coolingTicksAtRemoval, effectiveBreakOnHit, visual.get());
    }
    public static Optional<ItemStack> create(UUID resultId, UUID operatorId,
        ResourceLocation metalId, MetalPartDefinitionSnapshot snapshot,
        long heatingTicks, int heatingScore, long coolingTicksAtRemoval,
        int effectiveBreakOnHit, MetalVisualData visualData) {
        try {
            Item roughOutput = ForgeRegistries.ITEMS.getValue(snapshot.roughOutputItemId());
            if (!(roughOutput instanceof RoughMetalPartItem)) return Optional.empty();
            RoughMetalPartState state = new RoughMetalPartState(RoughMetalPartState.CURRENT_VERSION,
                resultId, operatorId, snapshot.definitionId(), metalId, snapshot.outputItemId(),
                snapshot.ingredientCount(), heatingTicks, heatingScore, coolingTicksAtRemoval,
                effectiveBreakOnHit, snapshot, visualData);
            ItemStack stack = new ItemStack(roughOutput);
            RoughMetalPartStateCodec.write(stack, state);
            return read(stack).isPresent() ? Optional.of(stack) : Optional.empty();
        } catch (RuntimeException exception) { return Optional.empty(); }
    }
}
