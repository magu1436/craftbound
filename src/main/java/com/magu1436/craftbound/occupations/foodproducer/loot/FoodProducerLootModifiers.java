package com.magu1436.craftbound.occupations.foodproducer.loot;

import com.magu1436.craftbound.Craftbound;
import com.mojang.serialization.Codec;

import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class FoodProducerLootModifiers {

    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Craftbound.MODID);

    public static final RegistryObject<Codec<CropHarvestLootModifier>> CROP_HARVEST =
            SERIALIZERS.register("crop_harvest", () -> CropHarvestLootModifier.CODEC);

    private FoodProducerLootModifiers() {
    }

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}
