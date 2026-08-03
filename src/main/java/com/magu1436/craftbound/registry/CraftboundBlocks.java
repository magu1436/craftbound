package com.magu1436.craftbound.registry;

import java.util.function.Supplier;

import com.magu1436.craftbound.Craftbound;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CraftboundBlocks {

    public static final DeferredRegister<Block> BLOCKS = 
            DeferredRegister.create(ForgeRegistries.BLOCKS, Craftbound.MODID);
    
    public static final RegistryObject<Block> SAMPLE_BLOCK = 
            registerBlock(
                "sample_block", 
                () -> new Block(
                    BlockBehaviour.Properties.copy(Blocks.SMITHING_TABLE).strength(3.5F)
                )
            );

    private static <T extends Block> RegistryObject<T> registerBlock (
        String name,
        Supplier<T> blockSupplier
    ) {
        RegistryObject<T> block = BLOCKS.register(name, blockSupplier);
        CraftboundItems.ITEMS.register(
            name,
            () -> new BlockItem(block.get(), new Item.Properties())
        );
        return block;
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
