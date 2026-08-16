package com.magu1436.craftbound.occupations.blacksmith.assembly;

import com.magu1436.craftbound.occupations.blacksmith.carving.definition.NonMetalPartDefinition;
import com.magu1436.craftbound.occupations.blacksmith.carving.definition.NonMetalPartDefinitions;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public record NonMetalPartAssemblyIngredient(ResourceLocation partTypeId,
    ResourceLocation materialId, boolean contributesToQuality) implements AssemblyIngredient {

    public NonMetalPartAssemblyIngredient {
        Objects.requireNonNull(partTypeId, "partTypeId");
        Objects.requireNonNull(materialId, "materialId");
    }

    @Override
    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) return false;
        NonMetalPartDefinition definition = NonMetalPartDefinitions.INSTANCE.get(partTypeId).orElse(null);
        return definition != null
            && materialId.equals(definition.materialProfileId())
            && definition.outputItemId().equals(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }
}
