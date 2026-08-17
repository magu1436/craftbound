package com.magu1436.craftbound.registry;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.blacksmith.assembly.QualityAssemblyRecipe;
import com.magu1436.craftbound.occupations.blacksmith.assembly.QualityAssemblyRecipeSerializer;
import com.magu1436.craftbound.occupations.blacksmith.assembly.QualityShapelessAssemblyRecipe;
import com.magu1436.craftbound.occupations.blacksmith.assembly.QualityShapelessAssemblyRecipeSerializer;
import com.magu1436.craftbound.occupations.foodproducer.ranch.RanchBlockRecipe;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CraftboundRecipeSerializers {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Craftbound.MODID);

    public static final RegistryObject<RecipeSerializer<RanchBlockRecipe>> RANCH_BLOCK =
        RECIPE_SERIALIZERS.register("ranch_block", RanchBlockRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<QualityAssemblyRecipe>> QUALITY_ASSEMBLY =
        RECIPE_SERIALIZERS.register("quality_assembly", QualityAssemblyRecipeSerializer::new);
    public static final RegistryObject<RecipeSerializer<QualityShapelessAssemblyRecipe>>
        QUALITY_SHAPELESS_ASSEMBLY = RECIPE_SERIALIZERS.register(
            "quality_shapeless_assembly", QualityShapelessAssemblyRecipeSerializer::new);
    private CraftboundRecipeSerializers() {
    }

    public static void register(IEventBus modEventBus) {
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
