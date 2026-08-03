package com.magu1436.craftbound.registry;

import com.magu1436.craftbound.Craftbound;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class CraftboundBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = 
            DeferredRegister.create(
                ForgeRegistries.BLOCK_ENTITY_TYPES, 
                Craftbound.MODID
            );
    
    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
