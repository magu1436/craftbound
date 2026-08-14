package com.magu1436.craftbound.registry;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.blacksmith.crucible.menu.CrucibleMenu;
import com.magu1436.craftbound.occupations.blacksmith.forging.menu.ForgingMenu;
import com.magu1436.craftbound.occupations.foodproducer.processing.FoodProcessingMenu;
import com.magu1436.craftbound.occupations.foodproducer.ranch.RanchMenu;
import com.magu1436.craftbound.occupations.foodproducer.storage.PreservationStorageMenu;

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

    public static final RegistryObject<MenuType<RanchMenu>> RANCH_BLOCK = MENUS.register(
        "ranch_block",
        () -> IForgeMenuType.create(RanchMenu::new)
    );
    public static final RegistryObject<MenuType<FoodProcessingMenu>> FOOD_PROCESSING = MENUS.register(
        "food_processing",
        () -> IForgeMenuType.create(FoodProcessingMenu::new)
    );
    public static final RegistryObject<MenuType<PreservationStorageMenu>> PRESERVATION_STORAGE = MENUS.register(
        "preservation_storage",
        () -> IForgeMenuType.create(PreservationStorageMenu::new)
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
