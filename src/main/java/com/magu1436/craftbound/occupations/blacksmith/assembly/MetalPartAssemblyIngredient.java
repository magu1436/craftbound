package com.magu1436.craftbound.occupations.blacksmith.assembly;

import com.magu1436.craftbound.occupations.blacksmith.casting.definition.MetalPartDefinition;
import com.magu1436.craftbound.occupations.blacksmith.casting.definition.MetalPartDefinitions;
import com.magu1436.craftbound.occupations.blacksmith.casting.finished.MetalPartStateService;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public record MetalPartAssemblyIngredient(ResourceLocation partTypeId,
    ResourceLocation materialId, boolean contributesToQuality) implements AssemblyIngredient {

    public MetalPartAssemblyIngredient {
        Objects.requireNonNull(partTypeId, "partTypeId");
        Objects.requireNonNull(materialId, "materialId");
    }

    @Override
    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) return false;
        MetalPartDefinition definition = MetalPartDefinitions.INSTANCE.get(partTypeId).orElse(null);
        if (definition == null
            || !definition.outputItemId().equals(ForgeRegistries.ITEMS.getKey(stack.getItem()))) {
            return false;
        }
        return MetalPartStateService.read(stack)
            .map(state -> materialId.equals(state.metalId()))
            .orElse(false);
    }
}
