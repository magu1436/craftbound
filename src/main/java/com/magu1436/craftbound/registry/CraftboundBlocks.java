package com.magu1436.craftbound.registry;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.blacksmith.casting.CastingTableBlock;
import com.magu1436.craftbound.occupations.blacksmith.forging.ForgingTableBlock;
import com.magu1436.craftbound.occupations.blacksmith.furnace.BlacksmithFurnaceBlock;
import com.magu1436.craftbound.occupations.blacksmith.carving.CarvingTableBlock;
import com.magu1436.craftbound.occupations.foodproducer.processing.FoodProcessingBlock;
import com.magu1436.craftbound.occupations.foodproducer.ranch.RanchBlock;
import com.magu1436.craftbound.occupations.foodproducer.storage.PreservationStorageBlock;
import com.magu1436.craftbound.occupations.foodproducer.foraging.RegionalForageBlock;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
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

    public static final RegistryObject<ForgingTableBlock> FORGING_TABLE =
            BLOCKS.register(
                "forging_table",
                () -> new ForgingTableBlock(
                    BlockBehaviour.Properties.of()
                        .strength(3.5F, 6.0F)
                        .sound(SoundType.STONE)
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

    public static final RegistryObject<CarvingTableBlock> CARVING_TABLE = BLOCKS.register(
        "carving_table", () -> new CarvingTableBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD))
    );

    public static final RegistryObject<Block> COOKING_TABLE = registerProcessingBlock(
        "cooking_table", MapColor.WOOD, SoundType.WOOD
    );
    public static final RegistryObject<Block> HAND_MILL = registerProcessingBlock(
        "hand_mill", MapColor.STONE, SoundType.STONE
    );
    public static final RegistryObject<Block> DRYING_RACK = registerProcessingBlock(
        "drying_rack", MapColor.WOOD, SoundType.WOOD
    );
    public static final RegistryObject<Block> COOKING_POT = registerProcessingBlock(
        "cooking_pot", MapColor.METAL, SoundType.METAL
    );
    public static final RegistryObject<Block> RANCH_BLOCK = BLOCKS.register(
        "ranch_block",
        () -> new RanchBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(2.5F)
            .sound(SoundType.WOOD))
    );
    public static final RegistryObject<Block> PRESERVATION_STORAGE_1 = BLOCKS.register(
        "preservation_storage_1",
        () -> new PreservationStorageBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.5F)
                .sound(SoundType.WOOD),
            3.0D
        )
    );
    public static final RegistryObject<Block> PRESERVATION_STORAGE_2 = BLOCKS.register(
        "preservation_storage_2",
        () -> new PreservationStorageBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5F)
                .sound(SoundType.METAL),
            5.0D
        )
    );

    public static final RegistryObject<Block> WILD_GARLIC = registerRegionalForage("wild_garlic");
    public static final RegistryObject<Block> FOREST_THYME = registerRegionalForage("forest_thyme");
    public static final RegistryObject<Block> JUNIPER_BERRY = registerRegionalForage("juniper_berry");
    public static final RegistryObject<Block> CACTUS_FIG = registerRegionalForage("cactus_fig");
    public static final RegistryObject<Block> WATER_CELERY = registerRegionalForage("water_celery");
    public static final RegistryObject<Block> JUNGLE_PEPPER = registerRegionalForage("jungle_pepper");
    public static final RegistryObject<Block> CHERRY_HERB = registerRegionalForage("cherry_herb");
    public static final RegistryObject<Block> ALPINE_LEEK = registerRegionalForage("alpine_leek");
    public static final RegistryObject<Block> MUSHROOM_TRUFFLE = registerRegionalForage("mushroom_truffle");
    public static final RegistryObject<Block> ICE_CRYSTAL_BERRY = registerRegionalForage("ice_crystal_berry");
    public static final RegistryObject<Block> BADLANDS_SAFFRON = registerRegionalForage("badlands_saffron");

    private static RegistryObject<Block> registerProcessingBlock(
        String id,
        MapColor color,
        SoundType sound
    ) {
        return BLOCKS.register(id, () -> new FoodProcessingBlock(
            BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(2.5F)
                .sound(sound)
        ));
    }

    private static RegistryObject<Block> registerRegionalForage(String id) {
        return BLOCKS.register(id, () -> new RegionalForageBlock(
            BlockBehaviour.Properties.copy(Blocks.GRASS)
                .noCollission()
                .strength(0.6F)
                .sound(SoundType.CROP)
                .offsetType(BlockBehaviour.OffsetType.XZ),
            new ResourceLocation(Craftbound.MODID, id)
        ));
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
