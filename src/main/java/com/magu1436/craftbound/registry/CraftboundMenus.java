package com.magu1436.craftbound.registry;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.blacksmith.crucible.menu.CrucibleMenu;
import com.magu1436.craftbound.occupations.blacksmith.forging.menu.ForgingMenu;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.common.extensions.IForgeMenuType;

public final class CraftboundMenus {

    public static final DeferredRegister<MenuType<?>> MENUS = 
            DeferredRegister.create(
                ForgeRegistries.MENU_TYPES,
                Craftbound.MODID
            );

    public static final RegistryObject<MenuType<CrucibleMenu>> CRUCIBLE =
        MENUS.register(
            "crucible",
            () -> IForgeMenuType.create(CrucibleMenu::new)
        );

    public static final RegistryObject<MenuType<ForgingMenu>> FORGING_TABLE =
        MENUS.register(
            "forging_table",
            () -> IForgeMenuType.create(ForgingMenu::new)
        );
    
    public static void  register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }

}
