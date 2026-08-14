package com.magu1436.craftbound.registry;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.blacksmith.casting.CastingTableBlockEntity;
import com.magu1436.craftbound.occupations.blacksmith.forging.ForgingTableBlockEntity;
import com.magu1436.craftbound.occupations.blacksmith.furnace.BlacksmithFurnaceBlockEntity;
import com.magu1436.craftbound.occupations.foodproducer.processing.FoodProcessingBlockEntity;
import com.magu1436.craftbound.occupations.foodproducer.ranch.RanchBlockEntity;
import com.magu1436.craftbound.occupations.foodproducer.storage.PreservationStorageBlockEntity;

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

    public static final RegistryObject<BlockEntityType<RanchBlockEntity>> RANCH_BLOCK =
        BLOCK_ENTITIES.register(
            "ranch_block",
            () -> BlockEntityType.Builder.of(
                RanchBlockEntity::new,
                CraftboundBlocks.RANCH_BLOCK.get()
            ).build(null)
        );
    public static final RegistryObject<BlockEntityType<FoodProcessingBlockEntity>> FOOD_PROCESSING =
        BLOCK_ENTITIES.register(
            "food_processing",
            () -> BlockEntityType.Builder.of(
                FoodProcessingBlockEntity::new,
                CraftboundBlocks.COOKING_TABLE.get(),
                CraftboundBlocks.HAND_MILL.get(),
                CraftboundBlocks.DRYING_RACK.get(),
                CraftboundBlocks.COOKING_POT.get()
            ).build(null)
        );
    public static final RegistryObject<BlockEntityType<PreservationStorageBlockEntity>>
        PRESERVATION_STORAGE = BLOCK_ENTITIES.register(
            "preservation_storage",
            () -> BlockEntityType.Builder.of(
                PreservationStorageBlockEntity::new,
                CraftboundBlocks.PRESERVATION_STORAGE_1.get(),
                CraftboundBlocks.PRESERVATION_STORAGE_2.get()
            ).build(null)
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

    public static final RegistryObject<BlockEntityType<CastingTableBlockEntity>>
            CASTING_TABLE = BLOCK_ENTITIES.register(
                "casting_table",
                () -> BlockEntityType.Builder.of(
                    CastingTableBlockEntity::new,
                    CraftboundBlocks.CASTING_TABLE.get()
                ).build(null)
            );
    
    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
