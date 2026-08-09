package com.magu1436.craftbound.registry;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.blacksmith.crucible.CrucibleItem;

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
    
    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
