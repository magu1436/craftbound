package com.magu1436.craftbound.registry;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.blacksmith.crucible.CrucibleItem;
import com.magu1436.craftbound.occupations.blacksmith.casting.lump.MetalLumpItem;
import com.magu1436.craftbound.occupations.blacksmith.casting.mold.CastingMoldItem;
import com.magu1436.craftbound.occupations.blacksmith.casting.part.RoughMetalPartItem;
import com.magu1436.craftbound.occupations.blacksmith.casting.finished.MetalPartItem;
import com.magu1436.craftbound.occupations.foodproducer.farming.AgriculturalFertilizerItem;
import com.magu1436.craftbound.occupations.foodproducer.foraging.RegionalIngredientItem;
import com.magu1436.craftbound.occupations.foodproducer.processing.FoodDishItem;
import com.magu1436.craftbound.occupations.foodproducer.processing.FoodIntermediateItem;
import com.magu1436.craftbound.occupations.foodproducer.processing.PreparedIngredientSetItem;
import com.magu1436.craftbound.occupations.foodproducer.storage.PreservationStorageBlockItem;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CraftboundItems {

    public static final DeferredRegister<Item> ITEMS = 
            DeferredRegister.create(ForgeRegistries.ITEMS, Craftbound.MODID);

    public static final RegistryObject<Item> CRUCIBLE = ITEMS.register(
            "crucible",
            () -> new CrucibleItem(new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<Item> SAMPLE_BLOCK = ITEMS.register(
            "sample_block",
            () -> new BlockItem(
                CraftboundBlocks.SAMPLE_BLOCK.get(),
                new Item.Properties()
            )
    );

    public static final RegistryObject<Item> BLACKSMITH_FURNACE = ITEMS.register(
            "blacksmith_furnace",
            () -> new BlockItem(
                CraftboundBlocks.BLACKSMITH_FURNACE.get(),
                new Item.Properties()
            )
    );
    public static final RegistryObject<Item> SMITHING_HAMMER = ITEMS.register(
            "smithing_hammer",
            () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final RegistryObject<Item> FORGING_TABLE = ITEMS.register(
            "forging_table",
            () -> new BlockItem(
                CraftboundBlocks.FORGING_TABLE.get(),
                new Item.Properties()
            )
    );
    public static final RegistryObject<Item> CASTING_TABLE = ITEMS.register(
            "casting_table",
            () -> new BlockItem(
                CraftboundBlocks.CASTING_TABLE.get(),
                new Item.Properties()
            )
    );
    public static final RegistryObject<Item> CARVING_TABLE = ITEMS.register(
        "carving_table", () -> new BlockItem(CraftboundBlocks.CARVING_TABLE.get(), new Item.Properties())
    );
    public static final RegistryObject<Item> CARVING_KNIFE = ITEMS.register(
        "carving_knife", () -> new Item(new Item.Properties().durability(128))
    );
    public static final RegistryObject<Item> CARVING_CHISEL = ITEMS.register(
        "carving_chisel", () -> new Item(new Item.Properties().durability(192))
    );
    public static final RegistryObject<Item> PICKAXE_HANDLE = ITEMS.register(
        "pickaxe_handle", () -> new Item(new Item.Properties().stacksTo(1))
    );
    public static final RegistryObject<Item> PROTECTIVE_LINING = registerItem("protective_lining");
    public static final RegistryObject<Item> BLANK_MOLD = registerItem("blank_mold");
    public static final RegistryObject<Item> SWORD_BLADE_MOLD = registerCastingMold("sword_blade_mold");
    public static final RegistryObject<Item> PICKAXE_HEAD_MOLD = registerCastingMold("pickaxe_head_mold");
    public static final RegistryObject<Item> AXE_HEAD_MOLD = registerCastingMold("axe_head_mold");
    public static final RegistryObject<Item> SHOVEL_HEAD_MOLD = registerCastingMold("shovel_head_mold");
    public static final RegistryObject<Item> HOE_HEAD_MOLD = registerCastingMold("hoe_head_mold");
    public static final RegistryObject<Item> HELMET_BODY_MOLD = registerCastingMold("helmet_body_mold");
    public static final RegistryObject<Item> CHESTPLATE_BODY_MOLD = registerCastingMold("chestplate_body_mold");
    public static final RegistryObject<Item> LEGGINGS_BODY_MOLD = registerCastingMold("leggings_body_mold");
    public static final RegistryObject<Item> BOOTS_BODY_MOLD = registerCastingMold("boots_body_mold");
    public static final RegistryObject<Item> HORSE_ARMOR_BODY_MOLD = registerCastingMold("horse_armor_body_mold");
    public static final RegistryObject<Item> IRON_RING_MOLD = registerCastingMold("iron_ring_mold");
    public static final RegistryObject<Item> CROSSBOW_TRIGGER_MOLD = registerCastingMold("crossbow_trigger_mold");
    public static final RegistryObject<Item> SHIELD_BOSS_MOLD = registerCastingMold("shield_boss_mold");
    public static final RegistryObject<Item> SHEARS_BLADES_MOLD = registerCastingMold("shears_blades_mold");
    public static final RegistryObject<Item> FIRE_STRIKER_MOLD = registerCastingMold("fire_striker_mold");
    public static final RegistryObject<Item> BRUSH_HEAD_MOLD = registerCastingMold("brush_head_mold");
    public static final RegistryObject<Item> ROUGH_SWORD_BLADE = registerRoughMetalPart("rough_sword_blade");
    public static final RegistryObject<Item> ROUGH_PICKAXE_HEAD = registerRoughMetalPart("rough_pickaxe_head");
    public static final RegistryObject<Item> ROUGH_AXE_HEAD = registerRoughMetalPart("rough_axe_head");
    public static final RegistryObject<Item> ROUGH_SHOVEL_HEAD = registerRoughMetalPart("rough_shovel_head");
    public static final RegistryObject<Item> ROUGH_HOE_HEAD = registerRoughMetalPart("rough_hoe_head");
    public static final RegistryObject<Item> ROUGH_HELMET_BODY = registerRoughMetalPart("rough_helmet_body");
    public static final RegistryObject<Item> ROUGH_CHESTPLATE_BODY = registerRoughMetalPart("rough_chestplate_body");
    public static final RegistryObject<Item> ROUGH_LEGGINGS_BODY = registerRoughMetalPart("rough_leggings_body");
    public static final RegistryObject<Item> ROUGH_BOOTS_BODY = registerRoughMetalPart("rough_boots_body");
    public static final RegistryObject<Item> ROUGH_HORSE_ARMOR_BODY = registerRoughMetalPart("rough_horse_armor_body");
    public static final RegistryObject<Item> ROUGH_IRON_RING = registerRoughMetalPart("rough_iron_ring");
    public static final RegistryObject<Item> ROUGH_CROSSBOW_TRIGGER = registerRoughMetalPart("rough_crossbow_trigger");
    public static final RegistryObject<Item> ROUGH_SHIELD_BOSS = registerRoughMetalPart("rough_shield_boss");
    public static final RegistryObject<Item> ROUGH_SHEARS_BLADES = registerRoughMetalPart("rough_shears_blades");
    public static final RegistryObject<Item> ROUGH_FIRE_STRIKER = registerRoughMetalPart("rough_fire_striker");
    public static final RegistryObject<Item> ROUGH_BRUSH_HEAD = registerRoughMetalPart("rough_brush_head");
    public static final RegistryObject<Item> SMALL_METAL_LUMP = ITEMS.register(
        "small_metal_lump", () -> new MetalLumpItem(new Item.Properties())
    );
    public static final RegistryObject<Item> SWORD_BLADE = registerMetalPart("sword_blade");
    public static final RegistryObject<Item> PICKAXE_HEAD = registerMetalPart("pickaxe_head");
    public static final RegistryObject<Item> AXE_HEAD = registerMetalPart("axe_head");
    public static final RegistryObject<Item> SHOVEL_HEAD = registerMetalPart("shovel_head");
    public static final RegistryObject<Item> HOE_HEAD = registerMetalPart("hoe_head");
    public static final RegistryObject<Item> HELMET_BODY = registerMetalPart("helmet_body");
    public static final RegistryObject<Item> CHESTPLATE_BODY = registerMetalPart("chestplate_body");
    public static final RegistryObject<Item> LEGGINGS_BODY = registerMetalPart("leggings_body");
    public static final RegistryObject<Item> BOOTS_BODY = registerMetalPart("boots_body");
    public static final RegistryObject<Item> HORSE_ARMOR_BODY = registerMetalPart("horse_armor_body");
    public static final RegistryObject<Item> IRON_RING = registerMetalPart("iron_ring");
    public static final RegistryObject<Item> CROSSBOW_TRIGGER = registerMetalPart("crossbow_trigger");
    public static final RegistryObject<Item> SHIELD_BOSS = registerMetalPart("shield_boss");
    public static final RegistryObject<Item> SHEARS_BLADES = registerMetalPart("shears_blades");
    public static final RegistryObject<Item> FIRE_STRIKER = registerMetalPart("fire_striker");
    public static final RegistryObject<Item> BRUSH_HEAD = registerMetalPart("brush_head");

    public static final RegistryObject<Item> AGRICULTURAL_FERTILIZER = ITEMS.register(
        "agricultural_fertilizer",
        () -> new AgriculturalFertilizerItem(new Item.Properties())
    );
    public static final RegistryObject<Item> COMPOST = registerItem("compost");
    public static final RegistryObject<Item> COOKING_KNIFE = ITEMS.register(
        "cooking_knife",
        () -> new Item(new Item.Properties().durability(64))
    );
    public static final RegistryObject<Item> BLACKSMITH_COOKING_KNIFE = ITEMS.register(
        "blacksmith_cooking_knife",
        () -> new Item(new Item.Properties().durability(512))
    );
    public static final RegistryObject<Item> WHEAT_FLOUR = registerFoodIntermediate("wheat_flour");
    public static final RegistryObject<Item> DOUGH = registerFoodIntermediate("dough");
    public static final RegistryObject<Item> SLICED_MEAT = registerFoodIntermediate("sliced_meat");
    public static final RegistryObject<Item> GROUND_MEAT = registerFoodIntermediate("ground_meat");
    public static final RegistryObject<Item> CHOPPED_VEGETABLE = registerFoodIntermediate("chopped_vegetable");
    public static final RegistryObject<Item> FRUIT_PIECES = registerFoodIntermediate("fruit_pieces");
    public static final RegistryObject<Item> DRIED_MEAT = registerFoodIntermediate("dried_meat");
    public static final RegistryObject<Item> DRIED_VEGETABLE = registerFoodIntermediate("dried_vegetable");
    public static final RegistryObject<Item> DRIED_FRUIT = registerFoodIntermediate("dried_fruit");
    public static final RegistryObject<Item> PREPARED_INGREDIENT_SET = ITEMS.register(
        "prepared_ingredient_set",
        () -> new PreparedIngredientSetItem(new Item.Properties())
    );
    public static final RegistryObject<Item> FOOD_DISH = ITEMS.register(
        "food_dish",
        () -> new FoodDishItem(new Item.Properties().stacksTo(16).food(
            new FoodProperties.Builder().nutrition(1).saturationMod(0.0F).build()
        ))
    );
    public static final RegistryObject<Item> COOKING_TABLE = registerBlockItem(
        "cooking_table", CraftboundBlocks.COOKING_TABLE
    );
    public static final RegistryObject<Item> HAND_MILL = registerBlockItem(
        "hand_mill", CraftboundBlocks.HAND_MILL
    );
    public static final RegistryObject<Item> DRYING_RACK = registerBlockItem(
        "drying_rack", CraftboundBlocks.DRYING_RACK
    );
    public static final RegistryObject<Item> COOKING_POT = registerBlockItem(
        "cooking_pot", CraftboundBlocks.COOKING_POT
    );
    public static final RegistryObject<Item> RANCH_BLOCK = registerBlockItem(
        "ranch_block", CraftboundBlocks.RANCH_BLOCK
    );
    public static final RegistryObject<Item> PRESERVATION_STORAGE_1 = ITEMS.register(
        "preservation_storage_1",
        () -> new PreservationStorageBlockItem(
            CraftboundBlocks.PRESERVATION_STORAGE_1.get(), new Item.Properties()
        )
    );
    public static final RegistryObject<Item> PRESERVATION_STORAGE_2 = ITEMS.register(
        "preservation_storage_2",
        () -> new PreservationStorageBlockItem(
            CraftboundBlocks.PRESERVATION_STORAGE_2.get(), new Item.Properties()
        )
    );
    public static final RegistryObject<Item> WILD_GARLIC = registerRegionalIngredient("wild_garlic");
    public static final RegistryObject<Item> FOREST_THYME = registerRegionalIngredient("forest_thyme");
    public static final RegistryObject<Item> JUNIPER_BERRY = registerRegionalIngredient("juniper_berry");
    public static final RegistryObject<Item> CACTUS_FIG = registerRegionalIngredient("cactus_fig");
    public static final RegistryObject<Item> WATER_CELERY = registerRegionalIngredient("water_celery");
    public static final RegistryObject<Item> JUNGLE_PEPPER = registerRegionalIngredient("jungle_pepper");
    public static final RegistryObject<Item> CHERRY_HERB = registerRegionalIngredient("cherry_herb");
    public static final RegistryObject<Item> ALPINE_LEEK = registerRegionalIngredient("alpine_leek");
    public static final RegistryObject<Item> MUSHROOM_TRUFFLE = registerRegionalIngredient("mushroom_truffle");
    public static final RegistryObject<Item> ICE_CRYSTAL_BERRY = registerRegionalIngredient("ice_crystal_berry");
    public static final RegistryObject<Item> BADLANDS_SAFFRON = registerRegionalIngredient("badlands_saffron");

    private static RegistryObject<Item> registerItem(String id) {
        return ITEMS.register(id, () -> new Item(new Item.Properties()));
    }

    private static RegistryObject<Item> registerCastingMold(String id) {
        return ITEMS.register(id, () -> new CastingMoldItem(new Item.Properties().stacksTo(1)));
    }

    private static RegistryObject<Item> registerRoughMetalPart(String id) {
        return ITEMS.register(id, () -> new RoughMetalPartItem(new Item.Properties().stacksTo(1)));
    }

    private static RegistryObject<Item> registerMetalPart(String id) {
        return ITEMS.register(id, () -> new MetalPartItem(new Item.Properties().stacksTo(1)));
    }

    private static RegistryObject<Item> registerFoodIntermediate(String id) {
        return ITEMS.register(id, () -> new FoodIntermediateItem(new Item.Properties()));
    }

    private static RegistryObject<Item> registerRegionalIngredient(String id) {
        return ITEMS.register(id, () -> new RegionalIngredientItem(new Item.Properties()));
    }

    private static RegistryObject<Item> registerBlockItem(
        String id,
        RegistryObject<? extends net.minecraft.world.level.block.Block> block
    ) {
        return ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }
    
    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
