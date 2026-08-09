package com.magu1436.craftbound.registry;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.blacksmith.crucible.CrucibleItem;
import com.magu1436.craftbound.occupations.blacksmith.casting.lump.MetalLumpItem;
import com.magu1436.craftbound.occupations.blacksmith.casting.mold.CastingMoldItem;
import com.magu1436.craftbound.occupations.blacksmith.casting.part.RoughMetalPartItem;

import net.minecraft.world.item.Item;
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
    public static final RegistryObject<Item> ROUGH_METAL_PART = ITEMS.register(
        "rough_metal_part", () -> new RoughMetalPartItem(new Item.Properties().stacksTo(1))
    );
    public static final RegistryObject<Item> SMALL_METAL_LUMP = ITEMS.register(
        "small_metal_lump", () -> new MetalLumpItem(new Item.Properties())
    );
    public static final RegistryObject<Item> IRON_PICKAXE_HEAD = registerItem("iron_pickaxe_head");

    private static RegistryObject<Item> registerItem(String id) {
        return ITEMS.register(id, () -> new Item(new Item.Properties()));
    }

    private static RegistryObject<Item> registerCastingMold(String id) {
        return ITEMS.register(id, () -> new CastingMoldItem(new Item.Properties().stacksTo(1)));
    }
    
    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
