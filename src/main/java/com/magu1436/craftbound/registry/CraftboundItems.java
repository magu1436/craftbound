package com.magu1436.craftbound.registry;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.blacksmith.crucible.CrucibleItem;
import com.magu1436.craftbound.occupations.blacksmith.casting.lump.MetalLumpItem;
import com.magu1436.craftbound.occupations.blacksmith.casting.mold.CastingMoldItem;
import com.magu1436.craftbound.occupations.blacksmith.casting.part.RoughMetalPartItem;
import com.magu1436.craftbound.occupations.blacksmith.casting.finished.MetalPartItem;

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
    public static final RegistryObject<Item> PICKAXE_HEAD = registerMetalPart("pickaxe_head");

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
    
    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
