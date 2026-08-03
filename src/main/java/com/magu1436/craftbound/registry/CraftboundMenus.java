package com.magu1436.craftbound.registry;

import com.magu1436.craftbound.Craftbound;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class CraftboundMenus {

    public static final DeferredRegister<MenuType<?>> MENUS = 
            DeferredRegister.create(
                ForgeRegistries.MENU_TYPES,
                Craftbound.MODID
            );
    
    public static void  register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }

}
