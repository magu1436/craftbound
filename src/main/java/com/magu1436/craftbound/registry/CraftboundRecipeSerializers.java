package com.magu1436.craftbound.registry;

import com.magu1436.craftbound.Craftbound;
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
    private CraftboundRecipeSerializers() {
    }

    public static void register(IEventBus modEventBus) {
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
