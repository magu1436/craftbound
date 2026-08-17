package com.magu1436.craftbound.loot;

import com.magu1436.craftbound.Craftbound;
import com.mojang.serialization.Codec;

import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Craftbound共通のGlobal Loot Modifier serializerを登録する. */
public final class CraftboundLootModifiers {

    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Craftbound.MODID);

    public static final RegistryObject<Codec<ReplaceItemLootModifier>> REPLACE_ITEM =
            SERIALIZERS.register("replace_item", () -> ReplaceItemLootModifier.CODEC);

    private CraftboundLootModifiers() {
    }

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}
