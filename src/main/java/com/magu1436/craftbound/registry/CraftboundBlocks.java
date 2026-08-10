package com.magu1436.craftbound.registry;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.blacksmith.furnace.BlacksmithFurnaceBlock;
import com.magu1436.craftbound.occupations.blacksmith.casting.CastingTableBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CraftboundBlocks {

    public static final DeferredRegister<Block> BLOCKS = 
            DeferredRegister.create(ForgeRegistries.BLOCKS, Craftbound.MODID);
    
    public static final RegistryObject<Block> SAMPLE_BLOCK = 
            BLOCKS.register(
                "sample_block", 
                () -> new Block(
                    BlockBehaviour.Properties.copy(Blocks.SMITHING_TABLE).strength(3.5F)
                )
            );

    public static final RegistryObject<BlacksmithFurnaceBlock> BLACKSMITH_FURNACE =
            BLOCKS.register(
                "blacksmith_furnace",
                () -> new BlacksmithFurnaceBlock(
                    BlockBehaviour.Properties.of()
                        .strength(3.5F, 6.0F)
                        .sound(SoundType.METAL)
                        .requiresCorrectToolForDrops()
                )
            );

    public static final RegistryObject<CastingTableBlock> CASTING_TABLE =
            BLOCKS.register(
                "casting_table",
                () -> new CastingTableBlock(
                    BlockBehaviour.Properties.of()
                        .strength(3.5F, 6.0F)
                        .sound(SoundType.METAL)
                        .requiresCorrectToolForDrops()
                )
            );

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
