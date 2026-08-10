package com.magu1436.craftbound.registry;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.blacksmith.forging.ForgingTableBlockEntity;
import com.magu1436.craftbound.occupations.blacksmith.furnace.BlacksmithFurnaceBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CraftboundBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = 
            DeferredRegister.create(
                ForgeRegistries.BLOCK_ENTITY_TYPES, 
                Craftbound.MODID
            );

    public static final RegistryObject<BlockEntityType<BlacksmithFurnaceBlockEntity>>
            BLACKSMITH_FURNACE = BLOCK_ENTITIES.register(
                "blacksmith_furnace",
                () -> BlockEntityType.Builder.of(
                    BlacksmithFurnaceBlockEntity::new,
                    CraftboundBlocks.BLACKSMITH_FURNACE.get()
                ).build(null)
            );

    public static final RegistryObject<BlockEntityType<ForgingTableBlockEntity>>
            FORGING_TABLE = BLOCK_ENTITIES.register(
                "forging_table",
                () -> BlockEntityType.Builder.of(
                    ForgingTableBlockEntity::new,
                    CraftboundBlocks.FORGING_TABLE.get()
                ).build(null)
            );
    
    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
