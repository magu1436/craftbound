package com.magu1436.craftbound.occupations.blacksmith.casting.part;

import java.util.Optional;
import java.util.UUID;
import com.magu1436.craftbound.occupations.blacksmith.casting.definition.MetalPartDefinitionSnapshot;
import com.magu1436.craftbound.registry.CraftboundItems;
import net.minecraft.world.item.ItemStack;

public final class RoughMetalPartStateService {
    private RoughMetalPartStateService() {}
    public static Optional<RoughMetalPartState> read(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(CraftboundItems.ROUGH_METAL_PART.get())) return Optional.empty();
        return RoughMetalPartStateCodec.read(stack);
    }
    public static Optional<ItemStack> create(UUID resultId, UUID operatorId,
        MetalPartDefinitionSnapshot snapshot, long heatingTicks, int heatingScore,
        long coolingTicksAtRemoval, int effectiveBreakOnHit) {
        try {
            RoughMetalPartState state = new RoughMetalPartState(RoughMetalPartState.CURRENT_VERSION,
                resultId, operatorId, snapshot.definitionId(), snapshot.metalId(), snapshot.outputItemId(),
                snapshot.ingredientCount(), heatingTicks, heatingScore, coolingTicksAtRemoval,
                effectiveBreakOnHit, snapshot);
            ItemStack stack = new ItemStack(CraftboundItems.ROUGH_METAL_PART.get());
            RoughMetalPartStateCodec.write(stack, state);
            return read(stack).isPresent() ? Optional.of(stack) : Optional.empty();
        } catch (RuntimeException exception) { return Optional.empty(); }
    }
}
