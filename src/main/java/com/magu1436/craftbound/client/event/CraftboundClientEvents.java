package com.magu1436.craftbound.client.event;

import java.util.List;
import java.util.function.Supplier;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.blacksmith.client.CrucibleScreen;
import com.magu1436.craftbound.occupations.blacksmith.client.MetalRenderColorResolver;
import com.magu1436.craftbound.occupations.blacksmith.client.MetalPartColorHandler;
import com.magu1436.craftbound.occupations.blacksmith.client.RoughMetalPartColorHandler;
import com.magu1436.craftbound.registry.CraftboundItems;
import com.magu1436.craftbound.registry.CraftboundMenus;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

@Mod.EventBusSubscriber(
    modid = Craftbound.MODID,
    bus = Mod.EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT
)
public final class CraftboundClientEvents {

    private static final List<ScreenRegistration> SCREENS = List.of(
        screen(
            CraftboundMenus.CRUCIBLE::get,
            CrucibleScreen::new
        )
    );

    private CraftboundClientEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(
        FMLClientSetupEvent event
    ) {
        event.enqueueWork(() ->
            SCREENS.forEach(ScreenRegistration::register)
        );
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(RoughMetalPartColorHandler::getColor,
            CraftboundItems.ROUGH_SWORD_BLADE.get(), CraftboundItems.ROUGH_PICKAXE_HEAD.get(),
            CraftboundItems.ROUGH_AXE_HEAD.get(), CraftboundItems.ROUGH_SHOVEL_HEAD.get(),
            CraftboundItems.ROUGH_HOE_HEAD.get(), CraftboundItems.ROUGH_HELMET_BODY.get(),
            CraftboundItems.ROUGH_CHESTPLATE_BODY.get(), CraftboundItems.ROUGH_LEGGINGS_BODY.get(),
            CraftboundItems.ROUGH_BOOTS_BODY.get(), CraftboundItems.ROUGH_HORSE_ARMOR_BODY.get(),
            CraftboundItems.ROUGH_IRON_RING.get(), CraftboundItems.ROUGH_CROSSBOW_TRIGGER.get(),
            CraftboundItems.ROUGH_SHIELD_BOSS.get(), CraftboundItems.ROUGH_SHEARS_BLADES.get(),
            CraftboundItems.ROUGH_FIRE_STRIKER.get(), CraftboundItems.ROUGH_BRUSH_HEAD.get());
        event.register(MetalPartColorHandler::getColor,
            CraftboundItems.SWORD_BLADE.get(), CraftboundItems.PICKAXE_HEAD.get(),
            CraftboundItems.AXE_HEAD.get(), CraftboundItems.SHOVEL_HEAD.get(),
            CraftboundItems.HOE_HEAD.get(), CraftboundItems.HELMET_BODY.get(),
            CraftboundItems.CHESTPLATE_BODY.get(), CraftboundItems.LEGGINGS_BODY.get(),
            CraftboundItems.BOOTS_BODY.get(), CraftboundItems.HORSE_ARMOR_BODY.get(),
            CraftboundItems.IRON_RING.get(), CraftboundItems.CROSSBOW_TRIGGER.get(),
            CraftboundItems.SHIELD_BOSS.get(), CraftboundItems.SHEARS_BLADES.get(),
            CraftboundItems.FIRE_STRIKER.get(), CraftboundItems.BRUSH_HEAD.get());
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new ResourceManagerReloadListener() {
            @Override
            public void onResourceManagerReload(ResourceManager resourceManager) {
                MetalRenderColorResolver.clearCache();
            }
        });
    }

    private static <
        M extends AbstractContainerMenu,
        S extends AbstractContainerScreen<M>
    > ScreenRegistration screen(
        Supplier<MenuType<M>> menuType,
        MenuScreens.ScreenConstructor<M, S> constructor
    ) {
        return new TypedScreenRegistration<>(
            menuType,
            constructor
        );
    }

    private interface ScreenRegistration {

        void register();
    }

    private record TypedScreenRegistration<
        M extends AbstractContainerMenu,
        S extends AbstractContainerScreen<M>
    >(
        Supplier<MenuType<M>> menuType,
        MenuScreens.ScreenConstructor<M, S> constructor
    ) implements ScreenRegistration {

        @Override
        public void register() {
            MenuScreens.register(
                this.menuType.get(),
                this.constructor
            );
        }
    }
}
